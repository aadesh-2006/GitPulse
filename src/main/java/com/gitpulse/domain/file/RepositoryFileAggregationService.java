package com.gitpulse.domain.file;

import com.gitpulse.common.exception.ResourceNotFoundException;
import com.gitpulse.domain.contributor.Contributor;
import com.gitpulse.domain.contributor.ContributorJpaRepository;
import com.gitpulse.domain.file.dto.FilePrimaryContributorRow;
import com.gitpulse.domain.file.dto.RepositoryFileAggregationResult;
import com.gitpulse.domain.file.dto.RepositoryFileAggregationRow;
import com.gitpulse.domain.repository.Repository;
import com.gitpulse.domain.repository.RepositoryJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Materialization service for repository_files read model.
 *
 * Transaction & Consistency Design:
 * - Atomic Single-Transaction Rebuild: The entire aggregation for a repository runs within a single
 *   @Transactional boundary. If any step fails, the entire rebuild rolls back atomically, preventing
 *   the read model from being left in a partially materialized or corrupted state.
 * - Persistence Batching: Uses chunked saveAll() calls (batch size: 500) to optimize JDBC batch
 *   execution and Hibernate entity persistence within the transaction.
 * - Memory Profile: Heavy raw file-change records are aggregated database-side via PostgreSQL native
 *   queries (avoiding loading raw FileChange entities into JVM memory). However, the distinct file path
 *   projections and existing RepositoryFile entities for the target repository are held in heap during
 *   the execution of this transaction.
 */
@Service
public class RepositoryFileAggregationService {

    private static final Logger log = LoggerFactory.getLogger(RepositoryFileAggregationService.class);
    private static final int JDBC_BATCH_SIZE = 500;

    private final RepositoryJpaRepository repositoryJpaRepository;
    private final RepositoryFileJpaRepository repositoryFileJpaRepository;
    private final ContributorJpaRepository contributorJpaRepository;

    public RepositoryFileAggregationService(RepositoryJpaRepository repositoryJpaRepository,
                                            RepositoryFileJpaRepository repositoryFileJpaRepository,
                                            ContributorJpaRepository contributorJpaRepository) {
        this.repositoryJpaRepository = Objects.requireNonNull(repositoryJpaRepository, "repositoryJpaRepository must not be null");
        this.repositoryFileJpaRepository = Objects.requireNonNull(repositoryFileJpaRepository, "repositoryFileJpaRepository must not be null");
        this.contributorJpaRepository = Objects.requireNonNull(contributorJpaRepository, "contributorJpaRepository must not be null");
    }

    @Transactional
    public RepositoryFileAggregationResult aggregateRepositoryFiles(Long repositoryId) {
        log.info("Starting atomic file activity and churn aggregation for repositoryId={}", repositoryId);

        Repository repository = repositoryJpaRepository.findById(repositoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found with id: " + repositoryId));

        // 1. Execute database-side aggregation for file churn metrics and primary contributor attributions
        List<RepositoryFileAggregationRow> aggRows = repositoryFileJpaRepository.aggregateFilesByRepositoryId(repositoryId);
        List<FilePrimaryContributorRow> primaryContribRows = repositoryFileJpaRepository.findPrimaryContributorsByRepositoryId(repositoryId);

        // 2. Resolve primary contributor for each distinct file path deterministically
        Map<String, Long> primaryContributorByPath = new HashMap<>();
        for (FilePrimaryContributorRow cRow : primaryContribRows) {
            primaryContributorByPath.putIfAbsent(cRow.getFilePath(), cRow.getContributorId());
        }

        Set<Long> contributorIds = new HashSet<>(primaryContributorByPath.values());
        Map<Long, Contributor> contributorMap = contributorIds.isEmpty()
                ? Collections.emptyMap()
                : contributorJpaRepository.findAllById(contributorIds).stream()
                .collect(Collectors.toMap(Contributor::getId, Function.identity()));

        // 3. Load existing repository_files for this repository to support in-place updates & stale detection
        List<RepositoryFile> existingFiles = repositoryFileJpaRepository.findByRepositoryId(repositoryId);
        Map<String, RepositoryFile> existingByPath = existingFiles.stream()
                .collect(Collectors.toMap(RepositoryFile::getFilePath, Function.identity()));

        Set<String> processedPaths = new HashSet<>();
        List<RepositoryFile> toSave = new ArrayList<>();
        int createdCount = 0;
        int updatedCount = 0;

        for (RepositoryFileAggregationRow row : aggRows) {
            String filePath = row.getFilePath();
            processedPaths.add(filePath);

            FilePathParser.ParsedPath parsed = FilePathParser.parse(filePath);
            boolean isDeleted = row.isDeleted();
            Long primaryContribId = primaryContributorByPath.get(filePath);
            Contributor primaryContributor = primaryContribId != null ? contributorMap.get(primaryContribId) : null;

            Instant firstModifiedAt = row.getFirstModifiedAtInstant();
            Instant lastModifiedAt = row.getLastModifiedAtInstant();

            RepositoryFile file = existingByPath.get(filePath);
            if (file != null) {
                file.updateMetrics(
                        row.getTotalRevisions(),
                        row.getTotalAdditions(),
                        row.getTotalDeletions(),
                        row.getTotalChurn(),
                        isDeleted,
                        firstModifiedAt,
                        lastModifiedAt,
                        primaryContributor
                );
                updatedCount++;
                toSave.add(file);
            } else {
                file = new RepositoryFile(
                        repository,
                        filePath,
                        parsed.fileName(),
                        parsed.extension(),
                        parsed.directoryPath(),
                        row.getTotalRevisions(),
                        row.getTotalAdditions(),
                        row.getTotalDeletions(),
                        row.getTotalChurn(),
                        isDeleted,
                        firstModifiedAt,
                        lastModifiedAt,
                        primaryContributor
                );
                createdCount++;
                toSave.add(file);
            }
        }

        // 4. Purge stale rows that no longer exist in the freshly calculated aggregation
        List<RepositoryFile> staleFiles = existingFiles.stream()
                .filter(f -> !processedPaths.contains(f.getFilePath()))
                .toList();
        int deletedCount = staleFiles.size();
        if (!staleFiles.isEmpty()) {
            repositoryFileJpaRepository.deleteAllInBatch(staleFiles);
            log.info("Deleted {} stale repository_files for repositoryId={}", deletedCount, repositoryId);
        }

        // 5. Persist new and updated entities in JDBC batch chunks within the active transaction
        for (int i = 0; i < toSave.size(); i += JDBC_BATCH_SIZE) {
            int end = Math.min(i + JDBC_BATCH_SIZE, toSave.size());
            repositoryFileJpaRepository.saveAll(toSave.subList(i, end));
        }
        repositoryFileJpaRepository.flush();

        RepositoryFileAggregationResult result = new RepositoryFileAggregationResult(
                repositoryId,
                aggRows.size(),
                createdCount,
                updatedCount,
                deletedCount
        );

        log.info("Completed atomic file aggregation for repositoryId={}: totalProcessed={}, created={}, updated={}, deleted={}",
                repositoryId, aggRows.size(), createdCount, updatedCount, deletedCount);

        return result;
    }
}