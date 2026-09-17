package com.gitpulse.domain.commit.dto;

import com.gitpulse.domain.commit.Commit;
import com.gitpulse.domain.commit.CommitClassification;

import java.time.Instant;

public record CommitResponse(
        Long id,
        Long repositoryId,
        String githubCommitSha,
        String message,
        String authorName,
        String authorEmail,
        String authorUsername,
        Instant committedAt,
        Integer additions,
        Integer deletions,
        Integer totalChanges,
        String htmlUrl,
        CommitClassification classification,
        Instant createdAt
) {
    public static CommitResponse fromEntity(Commit commit) {
        if (commit == null) {
            return null;
        }
        return new CommitResponse(
                commit.getId(),
                commit.getRepository() != null ? commit.getRepository().getId() : null,
                commit.getGithubCommitSha(),
                commit.getMessage(),
                commit.getAuthorName(),
                commit.getAuthorEmail(),
                commit.getAuthorUsername(),
                commit.getCommittedAt(),
                commit.getAdditions(),
                commit.getDeletions(),
                commit.getTotalChanges(),
                commit.getHtmlUrl(),
                commit.getClassification(),
                commit.getCreatedAt()
        );
    }
}
