package com.gitpulse.domain.contributorfile.dto;

import com.gitpulse.domain.contributor.Contributor;

public record RepositoryFileOwnershipResponse(
        Long repositoryId,
        String filePath,
        long contributorCount,
        long totalRevisionsAcrossContributors,
        ContributorSummary topContributor,
        Double topContributorRevisionShare
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
}