package com.gitpulse.domain.contributorfile.dto;

import java.time.Instant;
import java.time.OffsetDateTime;

public interface RepositoryContributorFileAggregationRow {

    Long getContributorId();

    String getFilePath();

    long getTotalRevisions();

    long getTotalAdditions();

    long getTotalDeletions();

    long getTotalChurn();

    OffsetDateTime getFirstContributedAt();

    OffsetDateTime getLastContributedAt();

    default Instant getFirstContributedAtInstant() {
        return getFirstContributedAt() != null ? getFirstContributedAt().toInstant() : null;
    }

    default Instant getLastContributedAtInstant() {
        return getLastContributedAt() != null ? getLastContributedAt().toInstant() : null;
    }
}
