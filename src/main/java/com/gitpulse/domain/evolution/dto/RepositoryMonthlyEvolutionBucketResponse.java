package com.gitpulse.domain.evolution.dto;

import java.time.Instant;

/**
 * Monthly evolution bucket containing historical engineering activity metrics.
 */
public record RepositoryMonthlyEvolutionBucketResponse(
        Instant month,
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
    public static RepositoryMonthlyEvolutionBucketResponse fromRow(RepositoryMonthlyEvolutionRow row) {
        return new RepositoryMonthlyEvolutionBucketResponse(
                row.getBucketMonth() != null ? row.getBucketMonth().toInstant() : null,
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
