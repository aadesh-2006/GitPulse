package com.gitpulse.domain.evolution.dto;

/**
 * Signed absolute numeric delta (current - previous) across all evolution metrics.
 */
public record RepositoryEvolutionDeltaResponse(
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
    public static RepositoryEvolutionDeltaResponse compute(
            RepositoryEvolutionPeriodResponse current,
            RepositoryEvolutionPeriodResponse previous
    ) {
        return new RepositoryEvolutionDeltaResponse(
                current.totalCommits() - previous.totalCommits(),
                current.totalAdditions() - previous.totalAdditions(),
                current.totalDeletions() - previous.totalDeletions(),
                current.totalChurn() - previous.totalChurn(),
                current.activeContributors() - previous.activeContributors(),
                current.filesChanged() - previous.filesChanged(),
                current.featureCommits() - previous.featureCommits(),
                current.bugFixCommits() - previous.bugFixCommits(),
                current.refactorCommits() - previous.refactorCommits(),
                current.documentationCommits() - previous.documentationCommits(),
                current.testCommits() - previous.testCommits(),
                current.buildCommits() - previous.buildCommits(),
                current.configurationCommits() - previous.configurationCommits(),
                current.dependencyCommits() - previous.dependencyCommits(),
                current.otherCommits() - previous.otherCommits()
        );
    }
}
