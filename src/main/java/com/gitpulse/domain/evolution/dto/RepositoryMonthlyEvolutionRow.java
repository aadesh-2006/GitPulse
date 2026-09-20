package com.gitpulse.domain.evolution.dto;

import java.time.OffsetDateTime;

/**
 * Projection interface for native SQL monthly repository evolution queries.
 */
public interface RepositoryMonthlyEvolutionRow {

    OffsetDateTime getBucketMonth();

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
