package com.gitpulse.domain.filechange;

public enum FileChangeStatus {
    ADDED,
    MODIFIED,
    REMOVED,
    RENAMED,
    UNKNOWN;

    public static FileChangeStatus fromString(String status) {
        if (status == null || status.isBlank()) {
            return UNKNOWN;
        }
        return switch (status.trim().toLowerCase()) {
            case "added" -> ADDED;
            case "modified" -> MODIFIED;
            case "removed" -> REMOVED;
            case "renamed" -> RENAMED;
            default -> UNKNOWN;
        };
    }
}
