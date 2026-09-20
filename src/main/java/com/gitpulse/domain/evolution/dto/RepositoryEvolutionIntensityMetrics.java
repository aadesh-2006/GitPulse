package com.gitpulse.domain.evolution.dto;

/**
 * Activity intensity and average change metrics per commit for a requested historical period.
 */
public record RepositoryEvolutionIntensityMetrics(
        double averageChurnPerCommit,
        double averageFilesChangedPerCommit,
        double averageAdditionsPerCommit,
        double averageDeletionsPerCommit
) {
    public static RepositoryEvolutionIntensityMetrics fromRaw(RepositoryEvolutionCompositionRawMetrics raw) {
        if (raw.totalCommits() == 0) {
            return new RepositoryEvolutionIntensityMetrics(0.0, 0.0, 0.0, 0.0);
        }

        double total = (double) raw.totalCommits();
        return new RepositoryEvolutionIntensityMetrics(
                raw.totalChurn() / total,
                raw.filesChanged() / total,
                raw.totalAdditions() / total,
                raw.totalDeletions() / total
        );
    }
}
