package com.gitpulse.domain.evolution.dto;

/**
 * Projection interface for native SQL repository evolution period summary queries.
 */
public interface RepositoryEvolutionPeriodSummaryRow {

    long getTotalCommits();

    long getTotalAdditions();

    long getTotalDeletions();

    long getTotalChurn();

    long getActiveContributors();

    long getFilesChanged();

    long getFeatureCommits();

    long getBugFixCommits();

    long getRefactorCommits();

    long getDocumentationCommits();

    long getTestCommits();

    long getBuildCommits();

    long getConfigurationCommits();

    long getDependencyCommits();

    long getOtherCommits();
}
