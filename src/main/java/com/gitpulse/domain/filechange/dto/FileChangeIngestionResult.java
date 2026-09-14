package com.gitpulse.domain.filechange.dto;

public class FileChangeIngestionResult {

    private final int commitsProcessed;
    private final int commitsSkipped;
    private final int filesReceived;
    private final int filesInserted;
    private final int duplicatesEncountered;
    private final long durationMs;

    public FileChangeIngestionResult(int commitsProcessed,
                                     int commitsSkipped,
                                     int filesReceived,
                                     int filesInserted,
                                     int duplicatesEncountered,
                                     long durationMs) {
        this.commitsProcessed = commitsProcessed;
        this.commitsSkipped = commitsSkipped;
        this.filesReceived = filesReceived;
        this.filesInserted = filesInserted;
        this.duplicatesEncountered = duplicatesEncountered;
        this.durationMs = durationMs;
    }

    public int getCommitsProcessed() {
        return commitsProcessed;
    }

    public int getCommitsSkipped() {
        return commitsSkipped;
    }

    public int getFilesReceived() {
        return filesReceived;
    }

    public int getFilesInserted() {
        return filesInserted;
    }

    public int getDuplicatesEncountered() {
        return duplicatesEncountered;
    }

    public long getDurationMs() {
        return durationMs;
    }

    @Override
    public String toString() {
        return "FileChangeIngestionResult{" +
                "commitsProcessed=" + commitsProcessed +
                ", commitsSkipped=" + commitsSkipped +
                ", filesReceived=" + filesReceived +
                ", filesInserted=" + filesInserted +
                ", duplicatesEncountered=" + duplicatesEncountered +
                ", durationMs=" + durationMs +
                '}';
    }
}
