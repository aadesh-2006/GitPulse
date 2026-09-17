package com.gitpulse.domain.commit.dto;

import com.gitpulse.domain.commit.Commit;
import com.gitpulse.domain.commit.CommitClassification;
import com.gitpulse.domain.filechange.FileChange;

import java.time.Instant;
import java.util.List;

public record CommitDetailResponse(
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
        Instant createdAt,
        List<CommitFileChangeResponse> fileChanges
) {
    public static CommitDetailResponse fromEntity(Commit commit, List<FileChange> fileChanges) {
        if (commit == null) {
            return null;
        }
        List<CommitFileChangeResponse> fileChangeResponses = fileChanges != null
                ? fileChanges.stream().map(CommitFileChangeResponse::fromEntity).toList()
                : List.of();

        return new CommitDetailResponse(
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
                commit.getCreatedAt(),
                fileChangeResponses
        );
    }
}
