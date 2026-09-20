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
import com.gitpulse.config.observability.GitPulseMetrics;
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
    private final GitPulseMetrics gitPulseMetrics;

    public RepositoryAnalysisProcessor(AnalysisJobJpaRepository analysisJobJpaRepository,
                                       CommitIngestionService commitIngestionService,
                                       CommitClassificationPipelineService commitClassificationPipelineService,
                                       FileChangeIngestionService fileChangeIngestionService,
                                       ContributorAggregationService contributorAggregationService,
                                       RepositoryFileAggregationService repositoryFileAggregationService,
                                       RepositoryContributorFileAggregationService repositoryContributorFileAggregationService,
                                       RepositoryFileRiskMaterializationService repositoryFileRiskMaterializationService,
                                       RepositoryEvolutionCacheVersionService cacheVersionService,
                                       GitPulseMetrics gitPulseMetrics) {
        this.analysisJobJpaRepository = Objects.requireNonNull(analysisJobJpaRepository, "analysisJobJpaRepository must not be null");
        this.commitIngestionService = Objects.requireNonNull(commitIngestionService, "commitIngestionService must not be null");
        this.commitClassificationPipelineService = Objects.requireNonNull(commitClassificationPipelineService, "commitClassificationPipelineService must not be null");
        this.fileChangeIngestionService = Objects.requireNonNull(fileChangeIngestionService, "fileChangeIngestionService must not be null");
        this.contributorAggregationService = Objects.requireNonNull(contributorAggregationService, "contributorAggregationService must not be null");
        this.repositoryFileAggregationService = Objects.requireNonNull(repositoryFileAggregationService, "repositoryFileAggregationService must not be null");
        this.repositoryContributorFileAggregationService = Objects.requireNonNull(repositoryContributorFileAggregationService, "repositoryContributorFileAggregationService must not be null");
        this.repositoryFileRiskMaterializationService = Objects.requireNonNull(repositoryFileRiskMaterializationService, "repositoryFileRiskMaterializationService must not be null");
        this.cacheVersionService = Objects.requireNonNull(cacheVersionService, "cacheVersionService must not be null");
        this.gitPulseMetrics = Objects.requireNonNull(gitPulseMetrics, "gitPulseMetrics must not be null");
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
        String currentStage = null;
        long currentStageStartNanos = 0;

        try {
            // Transition state: PENDING / FAILED -> RUNNING
            job.markRunning();
            analysisJobJpaRepository.saveAndFlush(job);
            gitPulseMetrics.incrementJobStarted();
            gitPulseMetrics.incrementActiveJobs();

            if (isRetry) {
                log.info("Analysis job [id={}, repo={}] re-entering RUNNING state on Kafka retry attempt", jobId, repoFullName);
            } else {
                log.info("Analysis job [id={}, repo={}] transitioned to RUNNING", jobId, repoFullName);
            }

            // Stage 1: GitHub Commit Ingestion
            currentStage = GitPulseMetrics.STAGE_COMMIT_INGESTION;
            log.info("Analysis job [id={}, repo={}] starting stage [{}]", jobId, repoFullName, currentStage);
            currentStageStartNanos = System.nanoTime();
            CommitIngestionResult commitResult = commitIngestionService.ingestCommits(repositoryId, jobId);
            long stage1DurationNanos = System.nanoTime() - currentStageStartNanos;
            gitPulseMetrics.recordStageDuration(currentStage, stage1DurationNanos, true);
            gitPulseMetrics.recordCommitsIngested(commitResult.getCommitsInserted(), commitResult.getDuplicatesEncountered());

            log.info("Analysis job [id={}, repo={}] completed stage [{}] in {}ms: pages={}, received={}, inserted={}, duplicates={}",
                    jobId, repoFullName, stage1DurationNanos / 1_000_000, commitResult.getPagesProcessed(), commitResult.getCommitsReceived(),
                    commitResult.getCommitsInserted(), commitResult.getDuplicatesEncountered());

            // Stage 2: Materialized Commit Classification
            currentStage = GitPulseMetrics.STAGE_COMMIT_CLASSIFICATION;
            log.info("Analysis job [id={}, repo={}] starting stage [{}]", jobId, repoFullName, currentStage);
            currentStageStartNanos = System.nanoTime();
            CommitClassificationResult classificationResult = commitClassificationPipelineService.classifyCommits(repositoryId, jobId);
            long stage2DurationNanos = System.nanoTime() - currentStageStartNanos;
            gitPulseMetrics.recordStageDuration(currentStage, stage2DurationNanos, true);

            log.info("Analysis job [id={}, repo={}] completed stage [{}] in {}ms: processed={}, classified={}",
                    jobId, repoFullName, stage2DurationNanos / 1_000_000, classificationResult.totalCommitsProcessed(),
                    classificationResult.classifiedCount());

            // Stage 3: GitHub File Change Ingestion
            currentStage = GitPulseMetrics.STAGE_FILE_CHANGE_INGESTION;
            log.info("Analysis job [id={}, repo={}] starting stage [{}]", jobId, repoFullName, currentStage);
            currentStageStartNanos = System.nanoTime();
            FileChangeIngestionResult fileResult = fileChangeIngestionService.ingestFileChanges(repositoryId, jobId);
            long stage3DurationNanos = System.nanoTime() - currentStageStartNanos;
            gitPulseMetrics.recordStageDuration(currentStage, stage3DurationNanos, true);
            gitPulseMetrics.recordFileChangesIngested(fileResult.getFilesInserted(), fileResult.getDuplicatesEncountered());

            log.info("Analysis job [id={}, repo={}] completed stage [{}] in {}ms: commitsProcessed={}, commitsSkipped={}, filesReceived={}, filesInserted={}, duplicates={}",
                    jobId, repoFullName, stage3DurationNanos / 1_000_000, fileResult.getCommitsProcessed(), fileResult.getCommitsSkipped(),
                    fileResult.getFilesReceived(), fileResult.getFilesInserted(), fileResult.getDuplicatesEncountered());

            // Stage 4: Materialized Contributor Activity Attribution Aggregation
            currentStage = GitPulseMetrics.STAGE_CONTRIBUTOR_AGGREGATION;
            log.info("Analysis job [id={}, repo={}] starting stage [{}]", jobId, repoFullName, currentStage);
            currentStageStartNanos = System.nanoTime();
            ContributorAggregationResult contributorResult = contributorAggregationService.aggregateContributors(repositoryId, jobId);
            long stage4DurationNanos = System.nanoTime() - currentStageStartNanos;
            gitPulseMetrics.recordStageDuration(currentStage, stage4DurationNanos, true);
            gitPulseMetrics.recordRecordsMaterialized(GitPulseMetrics.TYPE_CONTRIBUTORS, contributorResult.getContributorsAggregated());

            log.info("Analysis job [id={}, repo={}] completed stage [{}] in {}ms: aggregated={}, created={}, attributionsCreated={}, attributionsUpdated={}",
                    jobId, repoFullName, stage4DurationNanos / 1_000_000, contributorResult.getContributorsAggregated(),
                    contributorResult.getContributorsCreated(), contributorResult.getAttributionsCreated(),
                    contributorResult.getAttributionsUpdated());

            // Stage 5: Materialized Repository File Activity & Code Churn Aggregation
            currentStage = GitPulseMetrics.STAGE_REPOSITORY_FILE_AGGREGATION;
            log.info("Analysis job [id={}, repo={}] starting stage [{}]", jobId, repoFullName, currentStage);
            currentStageStartNanos = System.nanoTime();
            RepositoryFileAggregationResult fileAggResult = repositoryFileAggregationService.aggregateRepositoryFiles(repositoryId);
            long stage5DurationNanos = System.nanoTime() - currentStageStartNanos;
            gitPulseMetrics.recordStageDuration(currentStage, stage5DurationNanos, true);
            gitPulseMetrics.recordRecordsMaterialized(GitPulseMetrics.TYPE_REPOSITORY_FILES, fileAggResult.totalFilesProcessed());

            log.info("Analysis job [id={}, repo={}] completed stage [{}] in {}ms: totalFiles={}, created={}, updated={}, deleted={}",
                    jobId, repoFullName, stage5DurationNanos / 1_000_000, fileAggResult.totalFilesProcessed(), fileAggResult.createdCount(),
                    fileAggResult.updatedCount(), fileAggResult.deletedCount());

            // Stage 6: Materialized Contributor-File Aggregation
            currentStage = GitPulseMetrics.STAGE_CONTRIBUTOR_FILE_AGGREGATION;
            log.info("Analysis job [id={}, repo={}] starting stage [{}]", jobId, repoFullName, currentStage);
            currentStageStartNanos = System.nanoTime();
            RepositoryContributorFileAggregationResult contributorFileResult =
                    repositoryContributorFileAggregationService.aggregateRepositoryContributorFiles(repositoryId);
            long stage6DurationNanos = System.nanoTime() - currentStageStartNanos;
            gitPulseMetrics.recordStageDuration(currentStage, stage6DurationNanos, true);
            gitPulseMetrics.recordRecordsMaterialized(GitPulseMetrics.TYPE_CONTRIBUTOR_FILES, contributorFileResult.totalRowsProcessed());

            log.info("Analysis job [id={}, repo={}] completed stage [{}] in {}ms: processed={}, created={}, updated={}, unchanged={}, deleted={}",
                    jobId, repoFullName, stage6DurationNanos / 1_000_000, contributorFileResult.totalRowsProcessed(), contributorFileResult.createdCount(),
                    contributorFileResult.updatedCount(), contributorFileResult.unchangedCount(), contributorFileResult.deletedCount());

            // Stage 7: Deterministic File Risk Score Materialization
            currentStage = GitPulseMetrics.STAGE_FILE_RISK_MATERIALIZATION;
            log.info("Analysis job [id={}, repo={}] starting stage [{}]", jobId, repoFullName, currentStage);
            Instant referenceTime = Objects.requireNonNull(
                    job.getCreatedAt(),
                    "Analysis job createdAt must not be null before processing"
            );
            currentStageStartNanos = System.nanoTime();
            RepositoryFileRiskMaterializationResult riskResult =
                    repositoryFileRiskMaterializationService.materializeFileRisks(repositoryId, referenceTime);
            long stage7DurationNanos = System.nanoTime() - currentStageStartNanos;
            gitPulseMetrics.recordStageDuration(currentStage, stage7DurationNanos, true);
            gitPulseMetrics.recordRecordsMaterialized(GitPulseMetrics.TYPE_RISK_SCORES, riskResult.totalFilesProcessed());

            log.info("Analysis job [id={}, repo={}] completed stage [{}] in {}ms: totalProcessed={}, updated={}, unchanged={}",
                    jobId, repoFullName, stage7DurationNanos / 1_000_000, riskResult.totalFilesProcessed(), riskResult.updatedCount(), riskResult.unchangedCount());

            // Transition state: RUNNING -> COMPLETED
            job.markCompleted();
            analysisJobJpaRepository.saveAndFlush(job);
            long totalDurationNanos = System.nanoTime() - jobStartNanos;
            gitPulseMetrics.incrementJobCompleted();
            gitPulseMetrics.recordJobDuration(totalDurationNanos, true);

            log.info("Analysis job [id={}, repo={}] completed successfully in {}ms (transitioned to COMPLETED)", jobId, repoFullName, totalDurationNanos / 1_000_000);

            // Invalidate repository evolution cache by incrementing repository cache version
            try {
                cacheVersionService.incrementVersion(repositoryId);
            } catch (Exception ex) {
                log.warn("Failed to increment evolution cache version for repo {} after job completion: {}", repositoryId, ex.getMessage());
            }

        } catch (Exception ex) {
            long totalDurationNanos = System.nanoTime() - jobStartNanos;
            if (currentStage != null && currentStageStartNanos > 0) {
                gitPulseMetrics.recordStageDuration(currentStage, System.nanoTime() - currentStageStartNanos, false);
            }
            gitPulseMetrics.incrementJobFailed();
            gitPulseMetrics.recordJobDuration(totalDurationNanos, false);

            log.error("Analysis job [id={}, repo={}] failed after {}ms: {}", jobId, repoFullName, totalDurationNanos / 1_000_000, ex.getMessage(), ex);
            job.markFailed(ex.getMessage());
            analysisJobJpaRepository.saveAndFlush(job);
            if (ex instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException("Analysis job execution failed: " + ex.getMessage(), ex);
        } finally {
            gitPulseMetrics.decrementActiveJobs();
        }
    }
}