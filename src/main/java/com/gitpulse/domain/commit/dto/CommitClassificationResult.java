package com.gitpulse.domain.commit.dto;

public record CommitClassificationResult(
        int totalCommitsProcessed,
        int classifiedCount,
        long durationMs
) {
}
