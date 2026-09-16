package com.gitpulse.domain.contributor.dto;

import java.time.Instant;
import java.time.OffsetDateTime;

public interface ContributorAggregationRow {

    String getEmail();

    String getUsername();

    String getName();

    int getTotalCommits();

    int getTotalAdditions();

    int getTotalDeletions();

    int getTotalChanges();

    OffsetDateTime getFirstCommittedAt();

    OffsetDateTime getLastCommittedAt();

    default Instant getFirstCommittedAtInstant() {
        return getFirstCommittedAt() != null ? getFirstCommittedAt().toInstant() : null;
    }

    default Instant getLastCommittedAtInstant() {
        return getLastCommittedAt() != null ? getLastCommittedAt().toInstant() : null;
    }
}
