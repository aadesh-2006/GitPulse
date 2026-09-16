package com.gitpulse.domain.file.dto;

public record RepositoryFileAggregationResult(
        Long repositoryId,
        int totalFilesProcessed,
        int createdCount,
        int updatedCount,
        int deletedCount
) {
}