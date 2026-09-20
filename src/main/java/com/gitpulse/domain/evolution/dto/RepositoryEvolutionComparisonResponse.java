package com.gitpulse.domain.evolution.dto;

/**
 * Top-level response comparing repository evolution across two distinct historical periods.
 */
public record RepositoryEvolutionComparisonResponse(
        Long repositoryId,
        RepositoryEvolutionPeriodResponse currentPeriod,
        RepositoryEvolutionPeriodResponse previousPeriod,
        RepositoryEvolutionDeltaResponse delta
) {
}
