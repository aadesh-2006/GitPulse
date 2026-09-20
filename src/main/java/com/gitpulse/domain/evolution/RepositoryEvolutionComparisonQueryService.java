package com.gitpulse.domain.evolution;

import com.gitpulse.common.exception.ResourceNotFoundException;
import com.gitpulse.domain.evolution.dto.RepositoryEvolutionComparisonResponse;
import com.gitpulse.domain.evolution.dto.RepositoryEvolutionDeltaResponse;
import com.gitpulse.domain.evolution.dto.RepositoryEvolutionPeriodResponse;
import com.gitpulse.domain.evolution.dto.RepositoryEvolutionPeriodSummaryRow;
import com.gitpulse.domain.repository.RepositoryJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class RepositoryEvolutionComparisonQueryService {

    private final RepositoryJpaRepository repositoryJpaRepository;
    private final RepositoryEvolutionJpaRepository evolutionJpaRepository;

    public RepositoryEvolutionComparisonQueryService(
            RepositoryJpaRepository repositoryJpaRepository,
            RepositoryEvolutionJpaRepository evolutionJpaRepository) {
        this.repositoryJpaRepository = Objects.requireNonNull(repositoryJpaRepository, "repositoryJpaRepository must not be null");
        this.evolutionJpaRepository = Objects.requireNonNull(evolutionJpaRepository, "evolutionJpaRepository must not be null");
    }

    public RepositoryEvolutionComparisonResponse compareRepositoryEvolution(
            Long repositoryId,
            Instant currentFrom,
            Instant currentTo,
            Instant previousFrom,
            Instant previousTo
    ) {
        Objects.requireNonNull(repositoryId, "repositoryId must not be null");

        if (currentFrom == null) {
            throw new IllegalArgumentException("The 'currentFrom' timestamp parameter is required");
        }
        if (currentTo == null) {
            throw new IllegalArgumentException("The 'currentTo' timestamp parameter is required");
        }
        if (previousFrom == null) {
            throw new IllegalArgumentException("The 'previousFrom' timestamp parameter is required");
        }
        if (previousTo == null) {
            throw new IllegalArgumentException("The 'previousTo' timestamp parameter is required");
        }

        if (!currentFrom.isBefore(currentTo)) {
            throw new IllegalArgumentException("The 'currentFrom' timestamp must be strictly before the 'currentTo' timestamp");
        }
        if (!previousFrom.isBefore(previousTo)) {
            throw new IllegalArgumentException("The 'previousFrom' timestamp must be strictly before the 'previousTo' timestamp");
        }

        if (!repositoryJpaRepository.existsById(repositoryId)) {
            throw new ResourceNotFoundException("Repository not found with id: " + repositoryId);
        }

        RepositoryEvolutionPeriodSummaryRow currentRow = evolutionJpaRepository.findPeriodSummary(repositoryId, currentFrom, currentTo);
        RepositoryEvolutionPeriodSummaryRow previousRow = evolutionJpaRepository.findPeriodSummary(repositoryId, previousFrom, previousTo);

        RepositoryEvolutionPeriodResponse currentPeriod = RepositoryEvolutionPeriodResponse.fromRow(currentFrom, currentTo, currentRow);
        RepositoryEvolutionPeriodResponse previousPeriod = RepositoryEvolutionPeriodResponse.fromRow(previousFrom, previousTo, previousRow);
        RepositoryEvolutionDeltaResponse delta = RepositoryEvolutionDeltaResponse.compute(currentPeriod, previousPeriod);

        return new RepositoryEvolutionComparisonResponse(
                repositoryId,
                currentPeriod,
                previousPeriod,
                delta
        );
    }
}
