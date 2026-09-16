package com.gitpulse.domain.file.dto;

import java.time.Instant;
import java.time.OffsetDateTime;

public interface FilePrimaryContributorRow {

    String getFilePath();

    Long getContributorId();

    int getContributionCount();

    OffsetDateTime getLatestContributionAt();

    default Instant getLatestContributionAtInstant() {
        return getLatestContributionAt() != null ? getLatestContributionAt().toInstant() : null;
    }
}