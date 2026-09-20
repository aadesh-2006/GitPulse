package com.gitpulse.domain.analysis.processor;

import com.gitpulse.domain.analysis.AnalysisJob;
import com.gitpulse.domain.analysis.AnalysisJobJpaRepository;
import com.gitpulse.domain.analysis.AnalysisJobStatus;
import com.gitpulse.domain.commit.CommitClassificationPipelineService;
import com.gitpulse.domain.commit.CommitIngestionService;
import com.gitpulse.domain.commit.dto.CommitClassificationResult;
import com.gitpulse.domain.commit.dto.CommitIngestionResult;
import com.gitpulse.domain.contributor.ContributorAggregationService;
import com.gitpulse.domain.contributor.dto.ContributorAggregationResult;
import com.gitpulse.domain.contributorfile.RepositoryContributorFileAggregationService;
import com.gitpulse.domain.contributorfile.dto.RepositoryContributorFileAggregationResult;
import com.gitpulse.domain.evolution.cache.RepositoryEvolutionCacheVersionService;
import com.gitpulse.domain.file.RepositoryFileAggregationService;
import com.gitpulse.domain.file.dto.RepositoryFileAggregationResult;
import com.gitpulse.domain.filechange.FileChangeIngestionService;
import com.gitpulse.domain.filechange.dto.FileChangeIngestionResult;
import com.gitpulse.domain.risk.RepositoryFileRiskMaterializationService;
import com.gitpulse.domain.risk.dto.RepositoryFileRiskMaterializationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Service
public class RepositoryAnalysisProcessor {

    private static final Logger log = LoggerFactory.getLogger(RepositoryAnalysisProcessor.class);

    private final AnalysisJobJpaRepository analysisJobJpaRepository;
    private final CommitIngestionService commitIngestionService;
    private final CommitClassificationPipelineService commitClassificationPipelineService;
    private final FileChangeIngestionService fileChangeIngestionService;
    private final ContributorAggregationService contributorAggregationService;
    private final RepositoryFileAggregationService repositoryFileAggregationService;
    private final RepositoryContributorFileAggregationService repositoryContributorFileAggregationService;
    private final RepositoryFileRiskMaterializationService repositoryFileRiskMaterializationService;
    private final RepositoryEvolutionCacheVersionService cacheVersionService;

    public RepositoryAnalysisProcessor(AnalysisJobJpaRepository analysisJobJpaRepository,
                                       CommitIngestionService commitIngestionService,
                                       CommitClassificationPipelineService commitClassificationPipelineService,
                                       FileChangeIngestionService fileChangeIngestionService,
                                       ContributorAggregationService contributorAggregationService,
                                       RepositoryFileAggregationService repositoryFileAggregationService,
                                       RepositoryContributorFileAggregationService repositoryContributorFileAggregationService,
                                       RepositoryFileRiskMaterializationService repositoryFileRiskMaterializationService,
                                       RepositoryEvolutionCacheVersionService cacheVersionService) {
        this.analysisJobJpaRepository = Objects.requireNonNull(analysisJobJpaRepository, "analysisJobJpaRepository must not be null");
        this.commitIngestionService = Objects.requireNonNull(commitIngestionService, "commitIngestionService must not be null");
        this.commitClassificationPipelineService = Objects.requireNonNull(commitClassificationPipelineService, "commitClassificationPipelineService must not be null");
        this.fileChangeIngestionService = Objects.requireNonNull(fileChangeIngestionService, "fileChangeIngestionService must not be null");
        this.contributorAggregationService = Objects.requireNonNull(contributorAggregationService, "contributorAggregationService must not be null");
        this.repositoryFileAggregationService = Objects.requireNonNull(repositoryFileAggregationService, "repositoryFileAggregationService must not be null");
        this.repositoryContributorFileAggregationService = Objects.requireNonNull(repositoryContributorFileAggregationService, "repositoryContributorFileAggregationService must not be null");
        this.repositoryFileRiskMaterializationService = Objects.requireNonNull(repositoryFileRiskMaterializationService, "repositoryFileRiskMaterializationService must not be null");
        this.cacheVersionService = Objects.requireNonNull(cacheVersionService, "cacheVersionService must not be null");
    }

    public void processJob(Long jobId) {
        Optional<AnalysisJob> optionalJob = analysisJobJpaRepository.findWithRepositoryById(jobId);

        if (optionalJob.isEmpty()) {
            log.warn("Cannot process analysis job: Job [id={}] not found in database", jobId);
            return;
        }

        AnalysisJob job = optionalJob.get();

        // Idempotency check: verify job is in PENDING state
        if (job.getStatus() == AnalysisJobStatus.COMPLETED) {
            log.info("Analysis job [id={}] is already COMPLETED. Skipping duplicate event processing.", jobId);
            return;
        }

        if (job.getStatus() == AnalysisJobStatus.RUNNING) {
            log.warn("Analysis job [id={}] is already RUNNING. Skipping duplicate concurrent processing.", jobId);
            return;
        }

        boolean isRetry = job.getStatus() == AnalysisJobStatus.FAILED;
        Long repositoryId = job.getRepository().getId();
        String repoFullName = job.getRepository().getFullName();
        long jobStartNanos = System.nanoTime();

        try {
            // Transition state: PENDING / FAILED -> RUNNING
            job.markRunning();
            analysisJobJpaRepository.saveAndFlush(job);
            if (isRetry) {
                log.info("Analysis job [id={}, repo={}] re-entering RUNNING state on Kafka retry attempt", jobId, repoFullName);
            } else {
                log.info("Analysis job [id={}, repo={}] transitioned to RUNNING", jobId, repoFullName);
            }

            // Stage 1: GitHub Commit Ingestion
            log.info("Analysis job [id={}, repo={}] starting stage [COMMIT_INGESTION]", jobId, repoFullName);
            long stage1Start = System.nanoTime();
            CommitIngestionResult commitResult = commitIngestionService.ingestCommits(repositoryId, jobId);
            long stage1DurationMs = (System.nanoTime() - stage1Start) / 1_000_000;

            log.info("Analysis job [id={}, repo={}] completed stage [COMMIT_INGESTION] in {}ms: pages={}, received={}, inserted={}, duplicates={}",
                    jobId, repoFullName, stage1DurationMs, commitResult.getPagesProcessed(), commitResult.getCommitsReceived(),
                    commitResult.getCommitsInserted(), commitResult.getDuplicatesEncountered());

            // Stage 2: Materialized Commit Classification
            log.info("Analysis job [id={}, repo={}] starting stage [COMMIT_CLASSIFICATION]", jobId, repoFullName);
            long stage2Start = System.nanoTime();
            CommitClassificationResult classificationResult = commitClassificationPipelineService.classifyCommits(repositoryId, jobId);
            long stage2DurationMs = (System.nanoTime() - stage2Start) / 1_000_000;

            log.info("Analysis job [id={}, repo={}] completed stage [COMMIT_CLASSIFICATION] in {}ms: processed={}, classified={}",
                    jobId, repoFullName, stage2DurationMs, classificationResult.totalCommitsProcessed(),
                    classificationResult.classifiedCount());

            // Stage 3: GitHub File Change Ingestion
            log.info("Analysis job [id={}, repo={}] starting stage [FILE_CHANGE_INGESTION]", jobId, repoFullName);
            long stage3Start = System.nanoTime();
            FileChangeIngestionResult fileResult = fileChangeIngestionService.ingestFileChanges(repositoryId, jobId);
            long stage3DurationMs = (System.nanoTime() - stage3Start) / 1_000_000;

            log.info("Analysis job [id={}, repo={}] completed stage [FILE_CHANGE_INGESTION] in {}ms: commitsProcessed={}, commitsSkipped={}, filesReceived={}, filesInserted={}, duplicates={}",
                    jobId, repoFullName, stage3DurationMs, fileResult.getCommitsProcessed(), fileResult.getCommitsSkipped(),
                    fileResult.getFilesReceived(), fileResult.getFilesInserted(), fileResult.getDuplicatesEncountered());

            // Stage 4: Materialized Contributor Activity Attribution Aggregation
            log.info("Analysis job [id={}, repo={}] starting stage [CONTRIBUTOR_AGGREGATION]", jobId, repoFullName);
            long stage4Start = System.nanoTime();
            ContributorAggregationResult contributorResult = contributorAggregationService.aggregateContributors(repositoryId, jobId);
            long stage4DurationMs = (System.nanoTime() - stage4Start) / 1_000_000;

            log.info("Analysis job [id={}, repo={}] completed stage [CONTRIBUTOR_AGGREGATION] in {}ms: aggregated={}, created={}, attributionsCreated={}, attributionsUpdated={}",
                    jobId, repoFullName, stage4DurationMs, contributorResult.getContributorsAggregated(),
                    contributorResult.getContributorsCreated(), contributorResult.getAttributionsCreated(),
                    contributorResult.getAttributionsUpdated());

            // Stage 5: Materialized Repository File Activity & Code Churn Aggregation
            log.info("Analysis job [id={}, repo={}] starting stage [REPOSITORY_FILE_AGGREGATION]", jobId, repoFullName);
            long stage5Start = System.nanoTime();
            RepositoryFileAggregationResult fileAggResult = repositoryFileAggregationService.aggregateRepositoryFiles(repositoryId);
            long stage5DurationMs = (System.nanoTime() - stage5Start) / 1_000_000;

            log.info("Analysis job [id={}, repo={}] completed stage [REPOSITORY_FILE_AGGREGATION] in {}ms: totalFiles={}, created={}, updated={}, deleted={}",
                    jobId, repoFullName, stage5DurationMs, fileAggResult.totalFilesProcessed(), fileAggResult.createdCount(),
                    fileAggResult.updatedCount(), fileAggResult.deletedCount());

            // Stage 6: Materialized Contributor-File Aggregation
            log.info("Analysis job [id={}, repo={}] starting stage [CONTRIBUTOR_FILE_AGGREGATION]", jobId, repoFullName);
            long stage6Start = System.nanoTime();
            RepositoryContributorFileAggregationResult contributorFileResult =
                    repositoryContributorFileAggregationService.aggregateRepositoryContributorFiles(repositoryId);
            long stage6DurationMs = (System.nanoTime() - stage6Start) / 1_000_000;

            log.info("Analysis job [id={}, repo={}] completed stage [CONTRIBUTOR_FILE_AGGREGATION] in {}ms: processed={}, created={}, updated={}, unchanged={}, deleted={}",
                    jobId, repoFullName, stage6DurationMs, contributorFileResult.totalRowsProcessed(), contributorFileResult.createdCount(),
                    contributorFileResult.updatedCount(), contributorFileResult.unchangedCount(), contributorFileResult.deletedCount());

            // Stage 7: Deterministic File Risk Score Materialization
            log.info("Analysis job [id={}, repo={}] starting stage [FILE_RISK_MATERIALIZATION]", jobId, repoFullName);
            Instant referenceTime = Objects.requireNonNull(
                    job.getCreatedAt(),
                    "Analysis job createdAt must not be null before processing"
            );
            long stage7Start = System.nanoTime();
            RepositoryFileRiskMaterializationResult riskResult =
                    repositoryFileRiskMaterializationService.materializeFileRisks(repositoryId, referenceTime);
            long stage7DurationMs = (System.nanoTime() - stage7Start) / 1_000_000;

            log.info("Analysis job [id={}, repo={}] completed stage [FILE_RISK_MATERIALIZATION] in {}ms: totalProcessed={}, updated={}, unchanged={}",
                    jobId, repoFullName, stage7DurationMs, riskResult.totalFilesProcessed(), riskResult.updatedCount(), riskResult.unchangedCount());

            // Transition state: RUNNING -> COMPLETED
            job.markCompleted();
            analysisJobJpaRepository.saveAndFlush(job);
            long totalDurationMs = (System.nanoTime() - jobStartNanos) / 1_000_000;
            log.info("Analysis job [id={}, repo={}] completed successfully in {}ms (transitioned to COMPLETED)", jobId, repoFullName, totalDurationMs);

            // Invalidate repository evolution cache by incrementing repository cache version
            try {
                cacheVersionService.incrementVersion(repositoryId);
            } catch (Exception ex) {
                log.warn("Failed to increment evolution cache version for repo {} after job completion: {}", repositoryId, ex.getMessage());
            }

        } catch (Exception ex) {
            long totalDurationMs = (System.nanoTime() - jobStartNanos) / 1_000_000;
            log.error("Analysis job [id={}, repo={}] failed after {}ms: {}", jobId, repoFullName, totalDurationMs, ex.getMessage(), ex);
            job.markFailed(ex.getMessage());
            analysisJobJpaRepository.saveAndFlush(job);
            if (ex instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException("Analysis job execution failed: " + ex.getMessage(), ex);
        }
    }
}