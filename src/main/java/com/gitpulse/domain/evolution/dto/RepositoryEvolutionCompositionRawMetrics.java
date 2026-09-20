package com.gitpulse.domain.evolution.dto;

/**
 * Raw aggregated evolution metrics for a requested historical period.
 */
public record RepositoryEvolutionCompositionRawMetrics(
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
    public static RepositoryEvolutionCompositionRawMetrics fromRow(RepositoryEvolutionPeriodSummaryRow row) {
        if (row == null) {
            return new RepositoryEvolutionCompositionRawMetrics(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
        }
        return new RepositoryEvolutionCompositionRawMetrics(
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
