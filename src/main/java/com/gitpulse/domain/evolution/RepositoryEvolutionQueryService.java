package com.gitpulse.domain.evolution;

import com.gitpulse.common.exception.ResourceNotFoundException;
import com.gitpulse.domain.evolution.dto.RepositoryEvolutionResponse;
import com.gitpulse.domain.evolution.dto.RepositoryMonthlyEvolutionBucketResponse;
import com.gitpulse.domain.evolution.dto.RepositoryMonthlyEvolutionRow;
import com.gitpulse.domain.repository.RepositoryJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class RepositoryEvolutionQueryService {

    private final RepositoryJpaRepository repositoryJpaRepository;
    private final RepositoryEvolutionJpaRepository evolutionJpaRepository;

    public RepositoryEvolutionQueryService(RepositoryJpaRepository repositoryJpaRepository,
                                           RepositoryEvolutionJpaRepository evolutionJpaRepository) {
        this.repositoryJpaRepository = Objects.requireNonNull(repositoryJpaRepository, "repositoryJpaRepository must not be null");
        this.evolutionJpaRepository = Objects.requireNonNull(evolutionJpaRepository, "evolutionJpaRepository must not be null");
    }

    public RepositoryEvolutionResponse getRepositoryEvolution(Long repositoryId, Instant from, Instant to) {
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

        List<RepositoryMonthlyEvolutionRow> rows = evolutionJpaRepository.findMonthlyEvolution(repositoryId, from, to);
        List<RepositoryMonthlyEvolutionBucketResponse> buckets = rows.stream()
                .map(RepositoryMonthlyEvolutionBucketResponse::fromRow)
                .toList();

        return new RepositoryEvolutionResponse(repositoryId, from, to, buckets);
    }
}
