package com.gitpulse.domain.file.dto;

import com.gitpulse.domain.contributor.Contributor;

public record PrimaryContributorSummaryResponse(
        Long id,
        String email,
        String username,
        String name,
        String avatarUrl
) {
    public static PrimaryContributorSummaryResponse fromEntity(Contributor contributor) {
        if (contributor == null) {
            return null;
        }
        return new PrimaryContributorSummaryResponse(
                contributor.getId(),
                contributor.getEmail(),
                contributor.getUsername(),
                contributor.getName(),
                contributor.getAvatarUrl()
        );
    }
}