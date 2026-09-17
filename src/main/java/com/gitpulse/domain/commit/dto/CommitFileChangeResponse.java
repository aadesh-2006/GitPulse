package com.gitpulse.domain.commit.dto;

import com.gitpulse.domain.filechange.FileChange;
import com.gitpulse.domain.filechange.FileChangeStatus;

import java.time.Instant;

public record CommitFileChangeResponse(
        Long id,
        Long commitId,
        String filePath,
        FileChangeStatus status,
        Integer additions,
        Integer deletions,
        Integer changes,
        String blobUrl,
        String rawUrl,
        Instant createdAt
) {
    public static CommitFileChangeResponse fromEntity(FileChange fileChange) {
        if (fileChange == null) {
            return null;
        }
        return new CommitFileChangeResponse(
                fileChange.getId(),
                fileChange.getCommit() != null ? fileChange.getCommit().getId() : null,
                fileChange.getFilePath(),
                fileChange.getStatus(),
                fileChange.getAdditions(),
                fileChange.getDeletions(),
                fileChange.getChanges(),
                fileChange.getBlobUrl(),
                fileChange.getRawUrl(),
                fileChange.getCreatedAt()
        );
    }
}
