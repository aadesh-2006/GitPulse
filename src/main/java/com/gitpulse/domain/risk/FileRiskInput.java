package com.gitpulse.domain.risk;

import java.time.Instant;

/**
 * Immutable input model representing a file's analytical metrics for risk scoring.
 *
 * @param totalRevisions              total number of commits modifying this file
 * @param totalChurn                  total lines added and deleted in this file
 * @param lastModifiedAt              timestamp of the most recent commit modifying this file
 * @param topContributorRevisionShare fraction of revisions authored by the file's top contributor (0.0 to 1.0)
 */
public record FileRiskInput(
        long totalRevisions,
        long totalChurn,
        Instant lastModifiedAt,
        double topContributorRevisionShare
) {
}