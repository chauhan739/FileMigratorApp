public final class PathCleaner {
    private PathCleaner() {
    }

    public static String cleanLine(String rawLine) {
        if (rawLine == null) {
            return "";
        }
        String line = rawLine.trim();
        return line.replaceAll("(?i)^(modified:|deleted:|new file:|renamed:|copied:|typechange:|unmerged:|added:|[MADRCUT?!]{1,2})\\s+", "").trim();
    }

    public static boolean isDeletion(String rawLine) {
        if (rawLine == null) {
            return false;
        }
        String line = rawLine.trim().toLowerCase();
        return line.startsWith("deleted:") || line.startsWith("d:");
    }
}
