package com.gitpulse.domain.risk.dto;

/**
 * Immutable summary result of a repository file risk materialization execution.
 *
 * @param repositoryId        id of the repository processed
 * @param totalFilesProcessed total number of repository files evaluated
 * @param updatedCount        number of files whose risk scores were updated in the database
 * @param unchangedCount      number of files whose risk scores were already up to date
 */
public record RepositoryFileRiskMaterializationResult(
        Long repositoryId,
        int totalFilesProcessed,
        int updatedCount,
        int unchangedCount
) {
}