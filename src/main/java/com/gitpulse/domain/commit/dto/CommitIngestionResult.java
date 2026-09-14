package com.gitpulse.domain.commit.dto;

public class CommitIngestionResult {

    private final int pagesProcessed;
    private final int commitsReceived;
    private final int commitsInserted;
    private final int duplicatesEncountered;
    private final long durationMs;

    public CommitIngestionResult(int pagesProcessed,
                                 int commitsReceived,
                                 int commitsInserted,
                                 int duplicatesEncountered,
                                 long durationMs) {
        this.pagesProcessed = pagesProcessed;
        this.commitsReceived = commitsReceived;
        this.commitsInserted = commitsInserted;
        this.duplicatesEncountered = duplicatesEncountered;
        this.durationMs = durationMs;
    }

    public int getPagesProcessed() {
        return pagesProcessed;
    }

    public int getCommitsReceived() {
        return commitsReceived;
    }

    public int getCommitsInserted() {
        return commitsInserted;
    }

    public int getDuplicatesEncountered() {
        return duplicatesEncountered;
    }

    public long getDurationMs() {
        return durationMs;
    }

    @Override
    public String toString() {
        return "CommitIngestionResult{" +
                "pagesProcessed=" + pagesProcessed +
                ", commitsReceived=" + commitsReceived +
                ", commitsInserted=" + commitsInserted +
                ", duplicatesEncountered=" + duplicatesEncountered +
                ", durationMs=" + durationMs +
                '}';
    }
}
