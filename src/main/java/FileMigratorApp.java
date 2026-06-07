import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;

public class FileMigratorApp extends JFrame {
    private JTextField txtSource, txtTarget, txtListFile;
    private JTextArea txtDirectInput, logArea;
    private JTabbedPane inputTabs;

    public FileMigratorApp() {
        setAppIcon();
        setTitle("Git Path Migrator - chauhan739");
        setSize(700, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        Font customFont = new Font("Monospaced", Font.PLAIN, 15);

        // --- Top Panel: Source & Target Folders ---
        JPanel folderPanel = new JPanel(new GridLayout(2, 3, 5, 5));
        folderPanel.setBorder(BorderFactory.createTitledBorder("Step 1: Select Directories"));

        txtSource = new JTextField();
        txtTarget = new JTextField();

        folderPanel.add(new JLabel(" Source Folder:"));
        folderPanel.add(txtSource);
        folderPanel.add(createBrowseButton(txtSource, JFileChooser.DIRECTORIES_ONLY));

        folderPanel.add(new JLabel(" Target Folder:"));
        folderPanel.add(txtTarget);
        folderPanel.add(createBrowseButton(txtTarget, JFileChooser.DIRECTORIES_ONLY));

        // --- Center Panel: Input Methods (Tabs) ---
        inputTabs = new JTabbedPane();

        // Tab 1: File Selection
        JPanel fileTab = new JPanel(new FlowLayout(FlowLayout.LEFT));
        txtListFile = new JTextField(40);
        fileTab.add(new JLabel("Select List File:"));
        fileTab.add(txtListFile);
        fileTab.add(createBrowseButton(txtListFile, JFileChooser.FILES_ONLY));
        inputTabs.addTab("From File", fileTab);

        // Tab 2: Direct Text Input
        txtDirectInput = new JTextArea();
        txtDirectInput.setFont(customFont);
        inputTabs.addTab("Direct Paste (Enter paths here)", new JScrollPane(txtDirectInput));

        // --- Bottom Panel: Action & Logs ---
        JPanel bottomPanel = new JPanel(new BorderLayout());
        logArea = new JTextArea(10, 0);
        logArea.setFont(customFont);
        logArea.setEditable(false);
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(Color.GREEN);

        JButton btnRun = new JButton("Execute Migration");
        btnRun.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnRun.addActionListener(e -> startProcess());

        JButton btnClear = new JButton("Clear Log");
        btnClear.addActionListener(e -> logArea.setText(""));

        JPanel btnBar = new JPanel(new GridLayout(1, 2));
        btnBar.add(btnRun);
        btnBar.add(btnClear);

        bottomPanel.add(btnBar, BorderLayout.NORTH);
        bottomPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);

        // Layout Assembly
        add(folderPanel, BorderLayout.NORTH);
        add(inputTabs, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JButton createBrowseButton(JTextField field, int mode) {
        JButton btn = new JButton("Browse...");
        btn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(mode);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                field.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        return btn;
    }

    private void startProcess() {
        String srcBase = txtSource.getText().trim();
        String destBase = txtTarget.getText().trim();

        if (srcBase.isEmpty() || destBase.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please set source and target folders.");
            return;
        }

        File targetDir = new File(destBase);
        boolean wasInitiallyNotEmpty = false;

        // Check if target folder is not empty
        if (targetDir.exists() && targetDir.isDirectory()) {
            String[] files = targetDir.list();
            if (files != null && files.length > 0) {
                int choice = JOptionPane.showConfirmDialog(this,
                        "The target folder is not empty. Do you want to empty it before proceeding?",
                        "Target Folder Not Empty",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);

                if (choice == JOptionPane.YES_OPTION) {
                    logArea.append("Cleaning target folder...\n");
                    emptyFolder(targetDir);
                } else {
                    wasInitiallyNotEmpty = true; // Flag for the warning at the end
                }
            }
        }

        logArea.append("--- Starting Migration ---\n");

        // Execute the copy based on active tab
        if (inputTabs.getSelectedIndex() == 0) {
            processFromFile(srcBase, destBase);
        } else {
            processFromTextArea(srcBase, destBase);
        }

        // Final Warning if they chose NOT to empty the folder
        if (wasInitiallyNotEmpty) {
            JOptionPane.showMessageDialog(this,
                    "Warning: The target folder was not empty initially.\n" +
                            "Please re-verify the files to ensure no old files are conflicting.",
                    "Verification Required",
                    JOptionPane.WARNING_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Migration Completed Successfully!");
        }
    }

    /**
     * Recursively deletes everything inside the folder but keeps the folder itself.
     */
    private void emptyFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    emptyFolder(f);
                }
                f.delete();
            }
        }
    }

    private void processFromFile(String src, String dest) {
        File file = new File(txtListFile.getText());
        if (!file.exists()) {
            logArea.append("Error: List file not found.\n");
            return;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            br.lines().forEach(line -> copyPath(line.trim(), src, dest));
        } catch (IOException e) {
            logArea.append("Error reading file: " + e.getMessage() + "\n");
        }
    }

    private void processFromTextArea(String src, String dest) {
        String content = txtDirectInput.getText();
        if (content.isEmpty()) {
            logArea.append("Warning: Direct input is empty.\n");
            return;
        }
        String[] lines = content.split("\\n");
        for (String line : lines) {
            copyPath(line.trim(), src, dest);
        }
    }

    private void copyPath(String rawLine, String srcBase, String destBase) {
        String line = rawLine.trim();
        if (line.isEmpty()) return;

        // Clean Git prefixes and handle case-insensitivity
        String cleanPath = PathCleaner.cleanLine(line);

        // Skip deletions
        if (PathCleaner.isDeletion(line)) {
            logArea.append("SKIPPED (Deleted): " + cleanPath + "\n");
            return;
        }

        Path sourceRoot = Paths.get(srcBase, cleanPath);
        Path targetRoot = Paths.get(destBase, cleanPath);

        try {
            if (Files.exists(sourceRoot)) {
                if (Files.isDirectory(sourceRoot)) {
                    // RECURSIVE FOLDER COPY
                    Files.walk(sourceRoot).forEach(source -> {
                        try {
                            // Determine the relative path from the sourceRoot to the current file
                            Path target = targetRoot.resolve(sourceRoot.relativize(source));
                            if (Files.isDirectory(source)) {
                                Files.createDirectories(target);
                            } else {
                                Files.createDirectories(target.getParent());
                                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                            }
                        } catch (IOException e) {
                            logArea.append("FAILED to copy sub-item: " + source + "\n");
                        }
                    });
                    logArea.append("SUCCESS (Folder): " + cleanPath + "\n");
                } else {
                    // SINGLE FILE COPY
                    Files.createDirectories(targetRoot.getParent());
                    Files.copy(sourceRoot, targetRoot, StandardCopyOption.REPLACE_EXISTING);
                    logArea.append("SUCCESS (File): " + cleanPath + "\n");
                }
            } else {
                logArea.append("NOT FOUND: " + sourceRoot.toString() + "\n");
            }
        } catch (IOException e) {
            logArea.append("ERROR processing " + cleanPath + ": " + e.getMessage() + "\n");
        }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new FileMigratorApp().setVisible(true));
    }

    private void setAppIcon() {
        java.net.URL imgURL = getClass().getResource("/img.png");
        if (imgURL == null) {
            File f = new File("src/main/resources/img.png");
            if (!f.exists()) f = new File("../resources/img.png");
            if (f.exists()) {
                try { imgURL = f.toURI().toURL(); } catch (Exception ignored) {}
            }
        }

        if (imgURL != null) {
            setIconImage(new ImageIcon(imgURL).getImage());
        } else {
            // Drawing a badass fallback icon (a stylized 'M' for Migrator)
            BufferedImage canvas = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = canvas.createGraphics();

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Background Circle
            g2.setColor(new Color(30, 30, 30));
            g2.fillOval(0, 0, 63, 63);

            // Border
            g2.setStroke(new BasicStroke(3));
            g2.setColor(new Color(0, 255, 127)); // Spring Green
            g2.drawOval(2, 2, 59, 59);

            // The Letter M
            g2.setFont(new Font("Consolas", Font.BOLD, 40));
            g2.drawString("M", 18, 45);

            g2.dispose();
            setIconImage(canvas);
        }
    }
}
