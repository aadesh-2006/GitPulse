package com.gitpulse.domain.contributor.dto;

import com.gitpulse.domain.contributor.Contributor;

import java.time.Instant;

public record ContributorResponse(
        Long id,
        String email,
        String username,
        String name,
        String avatarUrl,
        Long githubId,
        Instant createdAt,
        Instant updatedAt
) {
    public static ContributorResponse fromEntity(Contributor entity) {
        if (entity == null) {
            return null;
        }
        return new ContributorResponse(
                entity.getId(),
                entity.getEmail(),
                entity.getUsername(),
                entity.getName(),
                entity.getAvatarUrl(),
                entity.getGithubId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
