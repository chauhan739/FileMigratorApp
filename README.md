# 📦 FileMigratorApp

A lightweight **Java Swing desktop utility** for migrating selected files and folders from a source directory to a target directory using a list of relative paths.

Built to simplify file migration workflows — especially useful when copying specific files from Git working trees.

---

## 🚀 Features

- 📁 Select **Source** and **Target** directories via GUI
- 📝 Dual Input Modes:
    - File Mode: Load a `.txt` or `.log` file containing paths.
    - Direct Paste: Paste raw `git status` output directly into the app.
- 🔄 Recursively copies files and folders
- 🧹 Option to clear target directory before migration
- 🧾 Real-time log output
- 🧠 Smart Path Cleaning: Automatically strips Git prefixes like
    - `modified:`
    - `new file:`
    - `deleted:` (skipped automatically)
    - `new file:`
    - `M:`
    - `A:` etc.

---

## 🛠 How It Works

1. **Select Folders:** Browse and select your Source (where files are now) and Target (where you want them to go).
2. **Provide Paths:**
   - Paste your git status output into the "Direct Paste" tab. 
   - Example input: `modified:   src/main/App.java`
3. **Execute:** Hit Execute Migration.
4. **Verification:** The app logs every success, skip, or failure in the green console.

---

## 🧱 Tech Stack

- Java
- Java Swing (GUI)
- Standard Java File I/O APIs

No external dependencies required.

---

## ⚠️ Notes
- **Deletions:** The app is smart enough to skip paths marked as `deleted:` to avoid errors, as those files no longer exist in the source.
- **Permissions:** Ensure the app has read/write access to the selected folders.

---

## 📌 Future Improvements (Ideas)

- Drag & drop support
- Export migration log to file 
- "Copy to Clipboard" button for the logs
- Preserve file timestamps 
- Ignore patterns (.gitignore style)
- Multi-threaded copy for performance 
- Progress bar indicator
- EXE build