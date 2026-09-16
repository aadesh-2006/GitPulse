package com.gitpulse.domain.contributor;

import com.gitpulse.common.exception.ResourceNotFoundException;
import com.gitpulse.domain.contributor.dto.ContributorAggregationResult;
import com.gitpulse.domain.contributor.dto.ContributorAggregationRow;
import com.gitpulse.domain.repository.Repository;
import com.gitpulse.domain.repository.RepositoryJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ContributorAggregationService {

    private static final Logger log = LoggerFactory.getLogger(ContributorAggregationService.class);

    private final RepositoryJpaRepository repositoryJpaRepository;
    private final ContributorJpaRepository contributorJpaRepository;
    private final RepositoryContributorJpaRepository repositoryContributorJpaRepository;

    public ContributorAggregationService(RepositoryJpaRepository repositoryJpaRepository,
                                         ContributorJpaRepository contributorJpaRepository,
                                         RepositoryContributorJpaRepository repositoryContributorJpaRepository) {
        this.repositoryJpaRepository = repositoryJpaRepository;
        this.contributorJpaRepository = contributorJpaRepository;
        this.repositoryContributorJpaRepository = repositoryContributorJpaRepository;
    }

    @Transactional
    public ContributorAggregationResult aggregateContributors(Long repositoryId, Long jobId) {
        Repository repository = repositoryJpaRepository.findById(repositoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository", "id", repositoryId));

        log.info("Starting contributor aggregation for repository [id={}, fullName={}, jobId={}]",
                repositoryId, repository.getFullName(), jobId);

        long startNanos = System.nanoTime();

        List<ContributorAggregationRow> rows = repositoryContributorJpaRepository.aggregateContributorsByRepositoryId(repositoryId);
        if (rows == null || rows.isEmpty()) {
            log.info("No commit authors found to aggregate for repository [id={}, jobId={}]", repositoryId, jobId);
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            return new ContributorAggregationResult(0, 0, 0, 0, durationMs);
        }

        Set<String> emails = rows.stream()
                .map(ContributorAggregationRow::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .collect(Collectors.toSet());

        Map<String, Contributor> contributorMap = new HashMap<>(
                contributorJpaRepository.findByEmailIn(emails).stream()
                        .collect(Collectors.toMap(Contributor::getEmail, Function.identity()))
        );

        int contributorsCreated = 0;
        List<Contributor> newContributors = new ArrayList<>();

        for (ContributorAggregationRow row : rows) {
            String email = row.getEmail();
            Contributor contributor = contributorMap.get(email);
            if (contributor == null) {
                contributor = new Contributor(email, row.getUsername(), row.getName());
                newContributors.add(contributor);
                contributorMap.put(email, contributor);
                contributorsCreated++;
            } else {
                contributor.updateProfile(row.getUsername(), row.getName());
            }
        }

        if (!newContributors.isEmpty()) {
            contributorJpaRepository.saveAll(newContributors);
        }

        Map<Long, RepositoryContributor> existingAttributions = repositoryContributorJpaRepository.findByRepositoryId(repositoryId).stream()
                .collect(Collectors.toMap(rc -> rc.getContributor().getId(), Function.identity()));

        int attributionsCreated = 0;
        int attributionsUpdated = 0;
        List<RepositoryContributor> attributionsToPersist = new ArrayList<>();

        for (ContributorAggregationRow row : rows) {
            Contributor contributor = contributorMap.get(row.getEmail());
            RepositoryContributor attribution = existingAttributions.get(contributor.getId());

            if (attribution == null) {
                attribution = new RepositoryContributor(
                        repository,
                        contributor,
                        row.getTotalCommits(),
                        row.getTotalAdditions(),
                        row.getTotalDeletions(),
                        row.getTotalChanges(),
                        row.getFirstCommittedAtInstant(),
                        row.getLastCommittedAtInstant()
                );
                attributionsCreated++;
            } else {
                attribution.updateMetrics(
                        row.getTotalCommits(),
                        row.getTotalAdditions(),
                        row.getTotalDeletions(),
                        row.getTotalChanges(),
                        row.getFirstCommittedAtInstant(),
                        row.getLastCommittedAtInstant()
                );
                attributionsUpdated++;
            }

            attributionsToPersist.add(attribution);
        }

        if (!attributionsToPersist.isEmpty()) {
            repositoryContributorJpaRepository.saveAll(attributionsToPersist);
        }

        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
        ContributorAggregationResult result = new ContributorAggregationResult(
                rows.size(),
                contributorsCreated,
                attributionsCreated,
                attributionsUpdated,
                durationMs
        );

        log.info("Completed contributor aggregation for repository [id={}, fullName={}, jobId={}]: {}",
                repositoryId, repository.getFullName(), jobId, result);

        return result;
    }
}
