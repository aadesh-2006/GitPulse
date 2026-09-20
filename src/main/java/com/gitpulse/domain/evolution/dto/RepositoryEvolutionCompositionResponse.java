package com.gitpulse.domain.evolution.dto;

import java.time.Instant;

/**
 * Top-level response for repository change composition and intensity analysis over a requested historical period.
 */
public record RepositoryEvolutionCompositionResponse(
        Long repositoryId,
        Instant from,
        Instant to,
        RepositoryEvolutionCompositionRawMetrics rawMetrics,
        RepositoryEvolutionCompositionShares composition,
        RepositoryEvolutionIntensityMetrics intensity
) {
}
