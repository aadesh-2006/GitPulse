package com.gitpulse.domain.commit;

import com.gitpulse.common.exception.ResourceNotFoundException;
import com.gitpulse.domain.commit.dto.CommitClassificationResult;
import com.gitpulse.domain.repository.Repository;
import com.gitpulse.domain.repository.RepositoryJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Pipeline stage service responsible for orchestrating deterministic commit classification
 * across all unclassified commits for a given repository.
 */
@Service
public class CommitClassificationPipelineService {

    private static final Logger log = LoggerFactory.getLogger(CommitClassificationPipelineService.class);
    private static final int BATCH_SIZE = 500;

    private final RepositoryJpaRepository repositoryJpaRepository;
    private final CommitJpaRepository commitJpaRepository;
    private final CommitClassificationService commitClassificationService;

    public CommitClassificationPipelineService(RepositoryJpaRepository repositoryJpaRepository,
                                               CommitJpaRepository commitJpaRepository,
                                               CommitClassificationService commitClassificationService) {
        this.repositoryJpaRepository = Objects.requireNonNull(repositoryJpaRepository, "repositoryJpaRepository must not be null");
        this.commitJpaRepository = Objects.requireNonNull(commitJpaRepository, "commitJpaRepository must not be null");
        this.commitClassificationService = Objects.requireNonNull(commitClassificationService, "commitClassificationService must not be null");
    }

    /**
     * Finds and classifies all unclassified commits for a repository in bounded batches.
     *
     * @param repositoryId the target repository ID
     * @param jobId the active analysis job ID (for traceability and logging)
     * @return summary result of the classification stage
     */
    @Transactional
    public CommitClassificationResult classifyCommits(Long repositoryId, Long jobId) {
        Repository repository = repositoryJpaRepository.findById(repositoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository", "id", repositoryId));

        log.info("Starting commit classification for repository [id={}, fullName={}, jobId={}]",
                repositoryId, repository.getFullName(), jobId);

        long startNanos = System.nanoTime();
        int totalProcessed = 0;
        int classifiedCount = 0;

        boolean hasMore = true;
        while (hasMore) {
            List<Commit> unclassified = commitJpaRepository.findUnclassifiedByRepositoryId(
                    repositoryId,
                    PageRequest.of(0, BATCH_SIZE)
            );

            if (unclassified == null || unclassified.isEmpty()) {
                break;
            }

            for (Commit commit : unclassified) {
                CommitClassification classification = commitClassificationService.classify(commit.getMessage());
                commit.setClassification(classification);
            }

            commitJpaRepository.saveAll(unclassified);
            totalProcessed += unclassified.size();
            classifiedCount += unclassified.size();

            log.debug("[jobId={}, repo={}] Classified and persisted batch of {} commits",
                    jobId, repository.getFullName(), unclassified.size());

            if (unclassified.size() < BATCH_SIZE) {
                hasMore = false;
            }
        }

        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
        CommitClassificationResult result = new CommitClassificationResult(totalProcessed, classifiedCount, durationMs);

        log.info("Completed commit classification for repository [id={}, fullName={}, jobId={}]: {}",
                repositoryId, repository.getFullName(), jobId, result);

        return result;
    }
}
