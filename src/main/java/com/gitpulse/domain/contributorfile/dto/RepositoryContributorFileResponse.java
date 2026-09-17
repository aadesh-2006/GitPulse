package com.gitpulse.domain.contributorfile.dto;

import com.gitpulse.domain.contributor.Contributor;
import com.gitpulse.domain.contributorfile.RepositoryContributorFile;

import java.time.Instant;

public record RepositoryContributorFileResponse(
        Long id,
        Long repositoryId,
        ContributorSummary contributor,
        String filePath,
        long totalRevisions,
        long totalAdditions,
        long totalDeletions,
        long totalChurn,
        Instant firstContributedAt,
        Instant lastContributedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public record ContributorSummary(
            Long id,
            String email,
            String username,
            String name,
            String avatarUrl
    ) {
        public static ContributorSummary fromEntity(Contributor contributor) {
            if (contributor == null) {
                return null;
            }
            return new ContributorSummary(
                    contributor.getId(),
                    contributor.getEmail(),
                    contributor.getUsername(),
                    contributor.getName(),
                    contributor.getAvatarUrl()
            );
        }
    }

    public static RepositoryContributorFileResponse fromEntity(RepositoryContributorFile entity) {
        if (entity == null) {
            return null;
        }
        return new RepositoryContributorFileResponse(
                entity.getId(),
                entity.getRepository() != null ? entity.getRepository().getId() : null,
                ContributorSummary.fromEntity(entity.getContributor()),
                entity.getFilePath(),
                entity.getTotalRevisions(),
                entity.getTotalAdditions(),
                entity.getTotalDeletions(),
                entity.getTotalChurn(),
                entity.getFirstContributedAt(),
                entity.getLastContributedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
