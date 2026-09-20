package com.gitpulse.domain.evolution;

import com.gitpulse.common.exception.ResourceNotFoundException;
import com.gitpulse.domain.evolution.dto.RepositoryEvolutionCompositionRawMetrics;
import com.gitpulse.domain.evolution.dto.RepositoryEvolutionCompositionResponse;
import com.gitpulse.domain.evolution.dto.RepositoryEvolutionCompositionShares;
import com.gitpulse.domain.evolution.dto.RepositoryEvolutionIntensityMetrics;
import com.gitpulse.domain.evolution.dto.RepositoryEvolutionPeriodSummaryRow;
import com.gitpulse.domain.repository.RepositoryJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class RepositoryEvolutionCompositionQueryService {

    private final RepositoryJpaRepository repositoryJpaRepository;
    private final RepositoryEvolutionJpaRepository evolutionJpaRepository;

    public RepositoryEvolutionCompositionQueryService(
            RepositoryJpaRepository repositoryJpaRepository,
            RepositoryEvolutionJpaRepository evolutionJpaRepository) {
        this.repositoryJpaRepository = Objects.requireNonNull(repositoryJpaRepository, "repositoryJpaRepository must not be null");
        this.evolutionJpaRepository = Objects.requireNonNull(evolutionJpaRepository, "evolutionJpaRepository must not be null");
    }

    public RepositoryEvolutionCompositionResponse getEvolutionComposition(Long repositoryId, Instant from, Instant to) {
        Objects.requireNonNull(repositoryId, "repositoryId must not be null");

        if (from == null) {
            throw new IllegalArgumentException("The 'from' timestamp parameter is required");
        }
        if (to == null) {
            throw new IllegalArgumentException("The 'to' timestamp parameter is required");
        }
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("The 'from' timestamp must be strictly before the 'to' timestamp");
        }

        if (!repositoryJpaRepository.existsById(repositoryId)) {
            throw new ResourceNotFoundException("Repository not found with id: " + repositoryId);
        }

        RepositoryEvolutionPeriodSummaryRow row = evolutionJpaRepository.findPeriodSummary(repositoryId, from, to);
        RepositoryEvolutionCompositionRawMetrics rawMetrics = RepositoryEvolutionCompositionRawMetrics.fromRow(row);
        RepositoryEvolutionCompositionShares composition = RepositoryEvolutionCompositionShares.fromRaw(rawMetrics);
        RepositoryEvolutionIntensityMetrics intensity = RepositoryEvolutionIntensityMetrics.fromRaw(rawMetrics);

        return new RepositoryEvolutionCompositionResponse(
                repositoryId,
                from,
                to,
                rawMetrics,
                composition,
                intensity
        );
    }
}
