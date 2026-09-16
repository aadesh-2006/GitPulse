package com.gitpulse.domain.contributor.dto;

import com.gitpulse.domain.contributor.RepositoryContributor;

import java.time.Instant;

public record RepositoryContributorResponse(
        Long id,
        Long repositoryId,
        ContributorResponse contributor,
        int totalCommits,
        int totalAdditions,
        int totalDeletions,
        int totalChanges,
        Instant firstCommittedAt,
        Instant lastCommittedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static RepositoryContributorResponse fromEntity(RepositoryContributor entity) {
        if (entity == null) {
            return null;
        }
        return new RepositoryContributorResponse(
                entity.getId(),
                entity.getRepository().getId(),
                ContributorResponse.fromEntity(entity.getContributor()),
                entity.getTotalCommits(),
                entity.getTotalAdditions(),
                entity.getTotalDeletions(),
                entity.getTotalChanges(),
                entity.getFirstCommittedAt(),
                entity.getLastCommittedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
