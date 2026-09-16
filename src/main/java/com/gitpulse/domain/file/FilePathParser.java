package com.gitpulse.domain.file;

public final class FilePathParser {

    private FilePathParser() {
    }

    public record ParsedPath(String fileName, String extension, String directoryPath) {
    }

    public static ParsedPath parse(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return new ParsedPath("", null, null);
        }

        String normalized = rawPath.replace('\\', '/').trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        if (normalized.isEmpty()) {
            return new ParsedPath("", null, null);
        }

        int lastSlash = normalized.lastIndexOf('/');
        String fileName;
        String directoryPath;

        if (lastSlash == -1) {
            fileName = normalized;
            directoryPath = null;
        } else {
            directoryPath = normalized.substring(0, lastSlash);
            fileName = normalized.substring(lastSlash + 1);
        }

        String extension = null;
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            extension = fileName.substring(lastDot + 1);
        }

        return new ParsedPath(fileName, extension, directoryPath);
    }
}