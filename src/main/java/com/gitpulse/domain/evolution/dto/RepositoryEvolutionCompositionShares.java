package com.gitpulse.domain.evolution.dto;

/**
 * Classification composition and category share metrics for a requested historical period.
 */
public record RepositoryEvolutionCompositionShares(
        long classifiedCommits,
        long unclassifiedCommits,
        double featureShare,
        double bugFixShare,
        double refactorShare,
        double documentationShare,
        double testShare,
        double buildShare,
        double configurationShare,
        double dependencyShare,
        double otherShare
) {
    public static RepositoryEvolutionCompositionShares fromRaw(RepositoryEvolutionCompositionRawMetrics raw) {
        long classified = raw.featureCommits()
                + raw.bugFixCommits()
                + raw.refactorCommits()
                + raw.documentationCommits()
                + raw.testCommits()
                + raw.buildCommits()
                + raw.configurationCommits()
                + raw.dependencyCommits()
                + raw.otherCommits();

        long unclassified = raw.totalCommits() - classified;

        if (classified == 0) {
            return new RepositoryEvolutionCompositionShares(
                    0L,
                    unclassified,
                    0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0
            );
        }

        double total = (double) classified;
        return new RepositoryEvolutionCompositionShares(
                classified,
                unclassified,
                raw.featureCommits() / total,
                raw.bugFixCommits() / total,
                raw.refactorCommits() / total,
                raw.documentationCommits() / total,
                raw.testCommits() / total,
                raw.buildCommits() / total,
                raw.configurationCommits() / total,
                raw.dependencyCommits() / total,
                raw.otherCommits() / total
        );
    }
}
