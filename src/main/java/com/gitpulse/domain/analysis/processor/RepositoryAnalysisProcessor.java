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
import com.gitpulse.domain.file.RepositoryFileAggregationService;
import com.gitpulse.domain.file.dto.RepositoryFileAggregationResult;
import com.gitpulse.domain.filechange.FileChangeIngestionService;
import com.gitpulse.domain.filechange.dto.FileChangeIngestionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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

    public RepositoryAnalysisProcessor(AnalysisJobJpaRepository analysisJobJpaRepository,
                                       CommitIngestionService commitIngestionService,
                                       CommitClassificationPipelineService commitClassificationPipelineService,
                                       FileChangeIngestionService fileChangeIngestionService,
                                       ContributorAggregationService contributorAggregationService,
                                       RepositoryFileAggregationService repositoryFileAggregationService) {
        this.analysisJobJpaRepository = Objects.requireNonNull(analysisJobJpaRepository, "analysisJobJpaRepository must not be null");
        this.commitIngestionService = Objects.requireNonNull(commitIngestionService, "commitIngestionService must not be null");
        this.commitClassificationPipelineService = Objects.requireNonNull(commitClassificationPipelineService, "commitClassificationPipelineService must not be null");
        this.fileChangeIngestionService = Objects.requireNonNull(fileChangeIngestionService, "fileChangeIngestionService must not be null");
        this.contributorAggregationService = Objects.requireNonNull(contributorAggregationService, "contributorAggregationService must not be null");
        this.repositoryFileAggregationService = Objects.requireNonNull(repositoryFileAggregationService, "repositoryFileAggregationService must not be null");
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

        if (job.getStatus() == AnalysisJobStatus.FAILED) {
            log.info("Analysis job [id={}] is in FAILED state. Skipping duplicate event processing.", jobId);
            return;
        }

        Long repositoryId = job.getRepository().getId();
        String repoFullName = job.getRepository().getFullName();

        try {
            // Transition state: PENDING -> RUNNING
            job.markRunning();
            analysisJobJpaRepository.saveAndFlush(job);
            log.info("Analysis job [id={}, repo={}] transitioned to RUNNING", jobId, repoFullName);

            // Stage 1: GitHub Commit Ingestion
            CommitIngestionResult commitResult = commitIngestionService.ingestCommits(repositoryId, jobId);

            log.info("Analysis job [id={}] commit ingestion finished in {}ms: pages={}, received={}, inserted={}, duplicates={}",
                    jobId, commitResult.getDurationMs(), commitResult.getPagesProcessed(), commitResult.getCommitsReceived(),
                    commitResult.getCommitsInserted(), commitResult.getDuplicatesEncountered());

            // Stage 2: Materialized Commit Classification
            CommitClassificationResult classificationResult = commitClassificationPipelineService.classifyCommits(repositoryId, jobId);

            log.info("Analysis job [id={}] commit classification finished in {}ms: processed={}, classified={}",
                    jobId, classificationResult.durationMs(), classificationResult.totalCommitsProcessed(),
                    classificationResult.classifiedCount());

            // Stage 3: GitHub File Change Ingestion
            FileChangeIngestionResult fileResult = fileChangeIngestionService.ingestFileChanges(repositoryId, jobId);

            log.info("Analysis job [id={}] file-change ingestion finished in {}ms: commitsProcessed={}, commitsSkipped={}, filesReceived={}, filesInserted={}, duplicates={}",
                    jobId, fileResult.getDurationMs(), fileResult.getCommitsProcessed(), fileResult.getCommitsSkipped(),
                    fileResult.getFilesReceived(), fileResult.getFilesInserted(), fileResult.getDuplicatesEncountered());

            // Stage 4: Materialized Contributor Activity Attribution Aggregation
            ContributorAggregationResult contributorResult = contributorAggregationService.aggregateContributors(repositoryId, jobId);

            log.info("Analysis job [id={}] contributor aggregation finished in {}ms: aggregated={}, created={}, attributionsCreated={}, attributionsUpdated={}",
                    jobId, contributorResult.getDurationMs(), contributorResult.getContributorsAggregated(),
                    contributorResult.getContributorsCreated(), contributorResult.getAttributionsCreated(),
                    contributorResult.getAttributionsUpdated());

            // Stage 5: Materialized Repository File Activity & Code Churn Aggregation
            RepositoryFileAggregationResult fileAggResult = repositoryFileAggregationService.aggregateRepositoryFiles(repositoryId);

            log.info("Analysis job [id={}] repository file aggregation finished: totalFiles={}, created={}, updated={}, deleted={}",
                    jobId, fileAggResult.totalFilesProcessed(), fileAggResult.createdCount(),
                    fileAggResult.updatedCount(), fileAggResult.deletedCount());

            // Transition state: RUNNING -> COMPLETED
            job.markCompleted();
            analysisJobJpaRepository.saveAndFlush(job);
            log.info("Analysis job [id={}, repo={}] transitioned to COMPLETED", jobId, repoFullName);

        } catch (Exception ex) {
            log.error("Error executing analysis for job [id={}]: {}", jobId, ex.getMessage(), ex);
            job.markFailed(ex.getMessage());
            analysisJobJpaRepository.saveAndFlush(job);
            if (ex instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException("Analysis job execution failed: " + ex.getMessage(), ex);
        }
    }
}