package com.gitpulse.domain.analysis.processor;

import com.gitpulse.domain.analysis.AnalysisJob;
import com.gitpulse.domain.analysis.AnalysisJobJpaRepository;
import com.gitpulse.domain.analysis.AnalysisJobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class RepositoryAnalysisProcessor {

    private static final Logger log = LoggerFactory.getLogger(RepositoryAnalysisProcessor.class);

    private final AnalysisJobJpaRepository analysisJobJpaRepository;

    public RepositoryAnalysisProcessor(AnalysisJobJpaRepository analysisJobJpaRepository) {
        this.analysisJobJpaRepository = analysisJobJpaRepository;
    }

    @Transactional
    public void processJob(Long jobId) {
        Optional<AnalysisJob> optionalJob = analysisJobJpaRepository.findById(jobId);

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

        try {
            // Transition state: PENDING -> RUNNING
            job.markRunning();
            analysisJobJpaRepository.save(job);
            log.info("Analysis job [id={}, repo={}] transitioned to RUNNING", jobId, job.getRepository().getFullName());

            // Placeholder for future analysis (commit ingestion, hotspot calculations)
            executeAnalysis(job);

            // Transition state: RUNNING -> COMPLETED
            job.markCompleted();
            analysisJobJpaRepository.save(job);
            log.info("Analysis job [id={}, repo={}] transitioned to COMPLETED", jobId, job.getRepository().getFullName());

        } catch (Exception ex) {
            log.error("Error executing analysis for job [id={}]: {}", jobId, ex.getMessage(), ex);
            job.markFailed(ex.getMessage());
            analysisJobJpaRepository.save(job);
        }
    }

    /**
     * Placeholder processing simulating future asynchronous analysis.
     * Real commit ingestion, diff parsing, and hotspot metrics will replace this in future milestones.
     */
    protected void executeAnalysis(AnalysisJob job) {
        log.info("Executing simulated repository analysis for [jobId={}, repo={}]",
                job.getId(), job.getRepository().getFullName());
        // Placeholder work for Step 4
    }
}
