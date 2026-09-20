package com.gitpulse.domain.file.dto;

import com.gitpulse.domain.file.RepositoryFile;

import java.time.Instant;

public record RepositoryFileResponse(
        Long id,
        Long repositoryId,
        String filePath,
        String fileName,
        String extension,
        String directoryPath,
        int totalRevisions,
        int totalAdditions,
        int totalDeletions,
        int totalChurn,
        boolean isDeleted,
        Instant firstModifiedAt,
        Instant lastModifiedAt,
        PrimaryContributorSummaryResponse primaryContributor,
        double baselineScore,
        double revisionFrequencyScore,
        double churnScore,
        double recencyScore,
        double ownershipConcentrationScore,
        double compositeScore,
        Instant createdAt,
        Instant updatedAt
) {
    public static RepositoryFileResponse fromEntity(RepositoryFile entity) {
        if (entity == null) {
            return null;
        }
        return new RepositoryFileResponse(
                entity.getId(),
                entity.getRepository().getId(),
                entity.getFilePath(),
                entity.getFileName(),
                entity.getExtension(),
                entity.getDirectoryPath(),
                entity.getTotalRevisions(),
                entity.getTotalAdditions(),
                entity.getTotalDeletions(),
                entity.getTotalChurn(),
                entity.isDeleted(),
                entity.getFirstModifiedAt(),
                entity.getLastModifiedAt(),
                PrimaryContributorSummaryResponse.fromEntity(entity.getPrimaryContributor()),
                entity.getBaselineScore(),
                entity.getRevisionFrequencyScore(),
                entity.getChurnScore(),
                entity.getRecencyScore(),
                entity.getOwnershipConcentrationScore(),
                entity.getCompositeScore(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}