package com.gitpulse.domain.evolution.dto;

import java.time.Instant;

/**
 * Evolution summary metrics for an explicit historical period.
 */
public record RepositoryEvolutionPeriodResponse(
        Instant from,
        Instant to,
        long totalCommits,
        long totalAdditions,
        long totalDeletions,
        long totalChurn,
        long activeContributors,
        long filesChanged,
        long featureCommits,
        long bugFixCommits,
        long refactorCommits,
        long documentationCommits,
        long testCommits,
        long buildCommits,
        long configurationCommits,
        long dependencyCommits,
        long otherCommits
) {
    public static RepositoryEvolutionPeriodResponse fromRow(Instant from, Instant to, RepositoryEvolutionPeriodSummaryRow row) {
        if (row == null) {
            return new RepositoryEvolutionPeriodResponse(
                    from, to, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L
            );
        }
        return new RepositoryEvolutionPeriodResponse(
                from,
                to,
                row.getTotalCommits(),
                row.getTotalAdditions(),
                row.getTotalDeletions(),
                row.getTotalChurn(),
                row.getActiveContributors(),
                row.getFilesChanged(),
                row.getFeatureCommits(),
                row.getBugFixCommits(),
                row.getRefactorCommits(),
                row.getDocumentationCommits(),
                row.getTestCommits(),
                row.getBuildCommits(),
                row.getConfigurationCommits(),
                row.getDependencyCommits(),
                row.getOtherCommits()
        );
    }
}
