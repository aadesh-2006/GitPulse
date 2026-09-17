package com.gitpulse.domain.contributorfile.dto;

public record RepositoryContributorFileAggregationResult(
        Long repositoryId,
        int totalRowsProcessed,
        int createdCount,
        int updatedCount,
        int unchangedCount,
        int deletedCount
) {
}
