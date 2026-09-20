package com.gitpulse.config.observability;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Central operational metrics service for GitPulse using Micrometer.
 * <p>
 * Exposes bounded telemetry for analysis jobs, execution stages, ingestion volumes,
 * materialized entity counts, Kafka retries, and Redis cache errors.
 * <p>
 * Strictly enforces bounded tag cardinalities (no IDs, URLs, paths, or keys as tags).
 */
@Component
public class GitPulseMetrics {

    // Metric names
    public static final String METRIC_ANALYSIS_JOBS = "gitpulse.analysis.jobs";
    public static final String METRIC_ANALYSIS_JOBS_ACTIVE = "gitpulse.analysis.jobs.active";
    public static final String METRIC_ANALYSIS_JOB_DURATION = "gitpulse.analysis.job.duration";
    public static final String METRIC_ANALYSIS_STAGE_DURATION = "gitpulse.analysis.stage.duration";
    public static final String METRIC_INGESTION_COMMITS = "gitpulse.ingestion.commits";
    public static final String METRIC_INGESTION_FILE_CHANGES = "gitpulse.ingestion.file.changes";
    public static final String METRIC_RECORDS_MATERIALIZED = "gitpulse.analysis.records.materialized";
    public static final String METRIC_KAFKA_RETRIES = "gitpulse.kafka.retries";
    public static final String METRIC_CACHE_ERRORS = "gitpulse.cache.errors";

    // Tag keys
    public static final String TAG_STATUS = "status";
    public static final String TAG_STAGE = "stage";
    public static final String TAG_RESULT = "result";
    public static final String TAG_TYPE = "type";
    public static final String TAG_OPERATION = "operation";

    // Tag values - Job Status
    public static final String STATUS_CREATED = "created";
    public static final String STATUS_STARTED = "started";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_FAILED = "failed";

    // Tag values - Ingestion Results
    public static final String RESULT_INSERTED = "inserted";
    public static final String RESULT_DUPLICATE = "duplicate";

    // Tag values - Materialized Entity Types
    public static final String TYPE_CONTRIBUTORS = "contributors";
    public static final String TYPE_REPOSITORY_FILES = "repository_files";
    public static final String TYPE_CONTRIBUTOR_FILES = "contributor_files";
    public static final String TYPE_RISK_SCORES = "risk_scores";

    // Tag values - Cache Operations
    public static final String OPERATION_READ = "read";
    public static final String OPERATION_WRITE = "write";

    // Tag values - Pipeline Stages
    public static final String STAGE_COMMIT_INGESTION = "COMMIT_INGESTION";
    public static final String STAGE_COMMIT_CLASSIFICATION = "COMMIT_CLASSIFICATION";
    public static final String STAGE_FILE_CHANGE_INGESTION = "FILE_CHANGE_INGESTION";
    public static final String STAGE_CONTRIBUTOR_AGGREGATION = "CONTRIBUTOR_AGGREGATION";
    public static final String STAGE_REPOSITORY_FILE_AGGREGATION = "REPOSITORY_FILE_AGGREGATION";
    public static final String STAGE_CONTRIBUTOR_FILE_AGGREGATION = "CONTRIBUTOR_FILE_AGGREGATION";
    public static final String STAGE_FILE_RISK_MATERIALIZATION = "FILE_RISK_MATERIALIZATION";

    private final MeterRegistry meterRegistry;
    private final AtomicInteger activeJobsCount = new AtomicInteger(0);

    public GitPulseMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry must not be null");
        this.meterRegistry.gauge(METRIC_ANALYSIS_JOBS_ACTIVE, activeJobsCount);
    }

    public void incrementJobCreated() {
        meterRegistry.counter(METRIC_ANALYSIS_JOBS, TAG_STATUS, STATUS_CREATED).increment();
    }

    public void incrementJobStarted() {
        meterRegistry.counter(METRIC_ANALYSIS_JOBS, TAG_STATUS, STATUS_STARTED).increment();
    }

    public void incrementJobCompleted() {
        meterRegistry.counter(METRIC_ANALYSIS_JOBS, TAG_STATUS, STATUS_COMPLETED).increment();
    }

    public void incrementJobFailed() {
        meterRegistry.counter(METRIC_ANALYSIS_JOBS, TAG_STATUS, STATUS_FAILED).increment();
    }

    public void incrementActiveJobs() {
        activeJobsCount.incrementAndGet();
    }

    public void decrementActiveJobs() {
        activeJobsCount.updateAndGet(count -> Math.max(0, count - 1));
    }

    public int getActiveJobsCount() {
        return activeJobsCount.get();
    }

    public void recordJobDuration(long durationNanos, boolean success) {
        meterRegistry.timer(METRIC_ANALYSIS_JOB_DURATION, TAG_STATUS, success ? STATUS_COMPLETED : STATUS_FAILED)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }

    public void recordStageDuration(String stage, long durationNanos, boolean success) {
        meterRegistry.timer(METRIC_ANALYSIS_STAGE_DURATION, TAG_STAGE, stage, TAG_STATUS, success ? STATUS_COMPLETED : STATUS_FAILED)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }

    public void recordCommitsIngested(long inserted, long duplicates) {
        if (inserted > 0) {
            meterRegistry.counter(METRIC_INGESTION_COMMITS, TAG_RESULT, RESULT_INSERTED).increment(inserted);
        }
        if (duplicates > 0) {
            meterRegistry.counter(METRIC_INGESTION_COMMITS, TAG_RESULT, RESULT_DUPLICATE).increment(duplicates);
        }
    }

    public void recordFileChangesIngested(long inserted, long duplicates) {
        if (inserted > 0) {
            meterRegistry.counter(METRIC_INGESTION_FILE_CHANGES, TAG_RESULT, RESULT_INSERTED).increment(inserted);
        }
        if (duplicates > 0) {
            meterRegistry.counter(METRIC_INGESTION_FILE_CHANGES, TAG_RESULT, RESULT_DUPLICATE).increment(duplicates);
        }
    }

    public void recordRecordsMaterialized(String type, long count) {
        if (count > 0) {
            meterRegistry.counter(METRIC_RECORDS_MATERIALIZED, TAG_TYPE, type).increment(count);
        }
    }

    public void incrementKafkaRetry() {
        meterRegistry.counter(METRIC_KAFKA_RETRIES).increment();
    }

    public void recordCacheError(String operation) {
        meterRegistry.counter(METRIC_CACHE_ERRORS, TAG_OPERATION, operation).increment();
    }
}
