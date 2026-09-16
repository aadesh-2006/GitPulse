package com.gitpulse.domain.file.dto;

import java.time.Instant;
import java.time.OffsetDateTime;

public interface RepositoryFileAggregationRow {

    String getFilePath();

    int getTotalRevisions();

    int getTotalAdditions();

    int getTotalDeletions();

    int getTotalChurn();

    OffsetDateTime getFirstModifiedAt();

    OffsetDateTime getLastModifiedAt();

    String getLastStatus();

    default Instant getFirstModifiedAtInstant() {
        return getFirstModifiedAt() != null ? getFirstModifiedAt().toInstant() : null;
    }

    default Instant getLastModifiedAtInstant() {
        return getLastModifiedAt() != null ? getLastModifiedAt().toInstant() : null;
    }

    default boolean isDeleted() {
        return "REMOVED".equalsIgnoreCase(getLastStatus());
    }
}