package com.gitpulse.benchmark.dto;

import java.time.Instant;
import java.time.OffsetDateTime;

public interface HistoricalFileSnapshotRow {

    String getFilePath();

    long getTotalRevisions();

    long getTotalChurn();

    OffsetDateTime getLastModifiedAt();

    default Instant getLastModifiedAtInstant() {
        return getLastModifiedAt() != null ? getLastModifiedAt().toInstant() : null;
    }
}
