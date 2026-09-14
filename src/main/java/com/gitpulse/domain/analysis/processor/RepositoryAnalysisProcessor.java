package com.gitpulse.domain.analysis.processor;

import com.gitpulse.domain.analysis.AnalysisJob;
import com.gitpulse.domain.analysis.AnalysisJobJpaRepository;
import com.gitpulse.domain.analysis.AnalysisJobStatus;
import com.gitpulse.domain.commit.CommitIngestionService;
import com.gitpulse.domain.commit.dto.CommitIngestionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RepositoryAnalysisProcessor {

    private static final Logger log = LoggerFactory.getLogger(RepositoryAnalysisProcessor.class);

    private final AnalysisJobJpaRepository analysisJobJpaRepository;
    private final CommitIngestionService commitIngestionService;

    public RepositoryAnalysisProcessor(AnalysisJobJpaRepository analysisJobJpaRepository,
                                       CommitIngestionService commitIngestionService) {
        this.analysisJobJpaRepository = analysisJobJpaRepository;
        this.commitIngestionService = commitIngestionService;
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

            // Real GitHub Commit Ingestion
            CommitIngestionResult result = commitIngestionService.ingestCommits(repositoryId, jobId);

            log.info("Analysis job [id={}] commit ingestion finished in {}ms: pages={}, received={}, inserted={}, duplicates={}",
                    jobId, result.getDurationMs(), result.getPagesProcessed(), result.getCommitsReceived(),
                    result.getCommitsInserted(), result.getDuplicatesEncountered());

            // Transition state: RUNNING -> COMPLETED
            job.markCompleted();
            analysisJobJpaRepository.saveAndFlush(job);
            log.info("Analysis job [id={}, repo={}] transitioned to COMPLETED", jobId, repoFullName);

        } catch (Exception ex) {
            log.error("Error executing analysis for job [id={}]: {}", jobId, ex.getMessage(), ex);
            job.markFailed(ex.getMessage());
            analysisJobJpaRepository.saveAndFlush(job);
        }
    }
}
