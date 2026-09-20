package com.gitpulse.domain.evolution.dto;

import java.time.Instant;
import java.util.List;

/**
 * Top-level response for repository evolution intelligence over a requested time range.
 */
public record RepositoryEvolutionResponse(
        Long repositoryId,
        Instant from,
        Instant to,
        List<RepositoryMonthlyEvolutionBucketResponse> buckets
) {
}
