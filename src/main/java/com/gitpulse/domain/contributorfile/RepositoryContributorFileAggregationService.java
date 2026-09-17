package com.gitpulse.domain.contributorfile;

import com.gitpulse.common.exception.ResourceNotFoundException;
import com.gitpulse.domain.contributor.Contributor;
import com.gitpulse.domain.contributor.ContributorJpaRepository;
import com.gitpulse.domain.contributorfile.dto.RepositoryContributorFileAggregationResult;
import com.gitpulse.domain.contributorfile.dto.RepositoryContributorFileAggregationRow;
import com.gitpulse.domain.repository.Repository;
import com.gitpulse.domain.repository.RepositoryJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Materialization service for repository_contributor_files read model.
 *
 * Transaction & Consistency Design:
 * - Atomic Single-Transaction Rebuild: The entire aggregation for a repository runs within a single
 *   @Transactional boundary. If any step fails, the entire rebuild rolls back atomically, preventing
 *   the read model from being left in a partially materialized or corrupted state.
 * - Bounded Aggregation Query Paging: The database-side GROUP BY aggregation rows are fetched in
 *   deterministic, bounded pages (page size: 500) ordered by (contributor_id ASC, file_path ASC).
 * - Bounded In-Place Reconciliation: Existing materialized rows are queried only for the keys present
 *   in each bounded aggregation page (at most 500 rows per batch), avoiding loading the full table into memory.
 * - Persistence Batching & Dirty Write Avoidance: Only newly created or genuinely changed rows are
 *   persisted in chunked saveAll() calls (batch size: 500). Unchanged rows remain untouched in the database.
 * - Database-Side Stale Row Purge: Stale contributor-file rows that no longer exist in the raw source data
 *   are purged directly via a targeted database-side NOT EXISTS deletion query without loading them into memory.
 */
@Service
public class RepositoryContributorFileAggregationService {

    private static final Logger log = LoggerFactory.getLogger(RepositoryContributorFileAggregationService.class);
    private static final int AGGREGATION_PAGE_SIZE = 500;
    private static final int JDBC_BATCH_SIZE = 500;

    private final RepositoryJpaRepository repositoryJpaRepository;
    private final RepositoryContributorFileJpaRepository repositoryContributorFileJpaRepository;
    private final ContributorJpaRepository contributorJpaRepository;

    public RepositoryContributorFileAggregationService(RepositoryJpaRepository repositoryJpaRepository,
                                                       RepositoryContributorFileJpaRepository repositoryContributorFileJpaRepository,
                                                       ContributorJpaRepository contributorJpaRepository) {
        this.repositoryJpaRepository = Objects.requireNonNull(repositoryJpaRepository, "repositoryJpaRepository must not be null");
        this.repositoryContributorFileJpaRepository = Objects.requireNonNull(repositoryContributorFileJpaRepository, "repositoryContributorFileJpaRepository must not be null");
        this.contributorJpaRepository = Objects.requireNonNull(contributorJpaRepository, "contributorJpaRepository must not be null");
    }

    @Transactional
    public RepositoryContributorFileAggregationResult aggregateRepositoryContributorFiles(Long repositoryId) {
        log.info("Starting atomic contributor-file intelligence aggregation for repositoryId={}", repositoryId);

        Repository repository = repositoryJpaRepository.findById(repositoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found with id: " + repositoryId));

        // 1. Purge stale materialized rows that no longer exist in the freshly calculated source aggregation
        int deletedCount = repositoryContributorFileJpaRepository.deleteStaleContributorFilesByRepositoryId(repositoryId);
        if (deletedCount > 0) {
            log.info("Deleted {} stale repository_contributor_files for repositoryId={}", deletedCount, repositoryId);
        }

        int totalRowsProcessed = 0;
        int createdCount = 0;
        int updatedCount = 0;
        int unchangedCount = 0;

        // 2. Fetch and reconcile aggregation rows in bounded pages ordered deterministically
        int pageNumber = 0;
        Page<RepositoryContributorFileAggregationRow> aggPage;

        do {
            aggPage = repositoryContributorFileJpaRepository.aggregateContributorFilesByRepositoryId(
                    repositoryId,
                    PageRequest.of(pageNumber, AGGREGATION_PAGE_SIZE)
            );

            List<RepositoryContributorFileAggregationRow> content = aggPage.getContent();
            if (content.isEmpty()) {
                break;
            }

            totalRowsProcessed += content.size();

            // Collect contributor IDs for this page to hydrate Contributor entities
            Set<Long> contributorIds = content.stream()
                    .map(RepositoryContributorFileAggregationRow::getContributorId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // Fetch any required Contributor entities for this page in batch
            Map<Long, Contributor> contributorMap = contributorIds.isEmpty()
                    ? Collections.emptyMap()
                    : contributorJpaRepository.findAllById(contributorIds).stream()
                    .collect(Collectors.toMap(Contributor::getId, Function.identity()));

            // Build exact composite keys (contributorId:::filePath) for this page
            List<String> compositeKeys = content.stream()
                    .map(r -> r.getContributorId() + ":::" + r.getFilePath())
                    .toList();

            // Fetch only the exact existing records matching this page's keys in batch
            Map<ContributorFileKey, RepositoryContributorFile> existingInBatch = compositeKeys.isEmpty()
                    ? Collections.emptyMap()
                    : repositoryContributorFileJpaRepository.findByRepositoryIdAndCompositeKeys(
                            repositoryId, compositeKeys
                    ).stream().collect(Collectors.toMap(
                            r -> new ContributorFileKey(r.getContributor().getId(), r.getFilePath()),
                            Function.identity()
                    ));

            List<RepositoryContributorFile> toSave = new ArrayList<>();

            for (RepositoryContributorFileAggregationRow row : content) {
                Long contributorId = row.getContributorId();
                String filePath = row.getFilePath();
                ContributorFileKey key = new ContributorFileKey(contributorId, filePath);

                Instant firstContributedAt = row.getFirstContributedAtInstant();
                Instant lastContributedAt = row.getLastContributedAtInstant();

                RepositoryContributorFile fileRecord = existingInBatch.get(key);
                if (fileRecord != null) {
                    if (fileRecord.hasMetricsChanged(
                            row.getTotalRevisions(),
                            row.getTotalAdditions(),
                            row.getTotalDeletions(),
                            row.getTotalChurn(),
                            firstContributedAt,
                            lastContributedAt
                    )) {
                        fileRecord.updateMetrics(
                                row.getTotalRevisions(),
                                row.getTotalAdditions(),
                                row.getTotalDeletions(),
                                row.getTotalChurn(),
                                firstContributedAt,
                                lastContributedAt
                        );
                        updatedCount++;
                        toSave.add(fileRecord);
                    } else {
                        unchangedCount++;
                    }
                } else {
                    Contributor contributor = contributorMap.get(contributorId);
                    if (contributor != null) {
                        fileRecord = new RepositoryContributorFile(
                                repository,
                                contributor,
                                filePath,
                                row.getTotalRevisions(),
                                row.getTotalAdditions(),
                                row.getTotalDeletions(),
                                row.getTotalChurn(),
                                firstContributedAt,
                                lastContributedAt
                        );
                        createdCount++;
                        toSave.add(fileRecord);
                    } else {
                        log.warn("Contributor id={} found in aggregation row for filePath='{}' but could not be resolved in database for repositoryId={}",
                                contributorId, filePath, repositoryId);
                    }
                }
            }

            // Persist page changes
            if (!toSave.isEmpty()) {
                for (int i = 0; i < toSave.size(); i += JDBC_BATCH_SIZE) {
                    int end = Math.min(i + JDBC_BATCH_SIZE, toSave.size());
                    repositoryContributorFileJpaRepository.saveAll(toSave.subList(i, end));
                }
            }

            pageNumber++;
        } while (aggPage.hasNext());

        RepositoryContributorFileAggregationResult result = new RepositoryContributorFileAggregationResult(
                repositoryId,
                totalRowsProcessed,
                createdCount,
                updatedCount,
                unchangedCount,
                deletedCount
        );

        log.info("Completed atomic contributor-file aggregation for repositoryId={}: totalProcessed={}, created={}, updated={}, unchanged={}, deleted={}",
                repositoryId, totalRowsProcessed, createdCount, updatedCount, unchangedCount, deletedCount);

        return result;
    }

    private record ContributorFileKey(Long contributorId, String filePath) {
    }
}
