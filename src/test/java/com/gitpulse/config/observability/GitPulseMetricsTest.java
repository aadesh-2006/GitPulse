package com.gitpulse.config.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class GitPulseMetricsTest {

    private SimpleMeterRegistry meterRegistry;
    private GitPulseMetrics metrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metrics = new GitPulseMetrics(meterRegistry);
    }

    @Test
    @DisplayName("Should track analysis job lifecycle counters correctly")
    void testAnalysisJobCounters() {
        metrics.incrementJobCreated();
        metrics.incrementJobStarted();
        metrics.incrementJobCompleted();
        metrics.incrementJobFailed();

        assertThat(meterRegistry.counter(GitPulseMetrics.METRIC_ANALYSIS_JOBS, GitPulseMetrics.TAG_STATUS, GitPulseMetrics.STATUS_CREATED).count())
                .isEqualTo(1.0);
        assertThat(meterRegistry.counter(GitPulseMetrics.METRIC_ANALYSIS_JOBS, GitPulseMetrics.TAG_STATUS, GitPulseMetrics.STATUS_STARTED).count())
                .isEqualTo(1.0);
        assertThat(meterRegistry.counter(GitPulseMetrics.METRIC_ANALYSIS_JOBS, GitPulseMetrics.TAG_STATUS, GitPulseMetrics.STATUS_COMPLETED).count())
                .isEqualTo(1.0);
        assertThat(meterRegistry.counter(GitPulseMetrics.METRIC_ANALYSIS_JOBS, GitPulseMetrics.TAG_STATUS, GitPulseMetrics.STATUS_FAILED).count())
                .isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should manage active jobs gauge without negative values")
    void testActiveJobsGauge() {
        assertThat(metrics.getActiveJobsCount()).isEqualTo(0);
        assertThat(meterRegistry.get(GitPulseMetrics.METRIC_ANALYSIS_JOBS_ACTIVE).gauge().value()).isEqualTo(0.0);

        metrics.incrementActiveJobs();
        metrics.incrementActiveJobs();
        assertThat(metrics.getActiveJobsCount()).isEqualTo(2);
        assertThat(meterRegistry.get(GitPulseMetrics.METRIC_ANALYSIS_JOBS_ACTIVE).gauge().value()).isEqualTo(2.0);

        metrics.decrementActiveJobs();
        assertThat(metrics.getActiveJobsCount()).isEqualTo(1);
        assertThat(meterRegistry.get(GitPulseMetrics.METRIC_ANALYSIS_JOBS_ACTIVE).gauge().value()).isEqualTo(1.0);

        metrics.decrementActiveJobs();
        metrics.decrementActiveJobs(); // Extra decrement should not go below zero
        assertThat(metrics.getActiveJobsCount()).isEqualTo(0);
        assertThat(meterRegistry.get(GitPulseMetrics.METRIC_ANALYSIS_JOBS_ACTIVE).gauge().value()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should record analysis job duration for completed and failed states")
    void testJobDurationTimer() {
        metrics.recordJobDuration(TimeUnit.MILLISECONDS.toNanos(150), true);
        metrics.recordJobDuration(TimeUnit.MILLISECONDS.toNanos(50), false);

        Timer completedTimer = meterRegistry.timer(GitPulseMetrics.METRIC_ANALYSIS_JOB_DURATION, GitPulseMetrics.TAG_STATUS, GitPulseMetrics.STATUS_COMPLETED);
        Timer failedTimer = meterRegistry.timer(GitPulseMetrics.METRIC_ANALYSIS_JOB_DURATION, GitPulseMetrics.TAG_STATUS, GitPulseMetrics.STATUS_FAILED);

        assertThat(completedTimer.count()).isEqualTo(1L);
        assertThat(failedTimer.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should record stage duration with stage and status tags")
    void testStageDurationTimer() {
        metrics.recordStageDuration(GitPulseMetrics.STAGE_COMMIT_INGESTION, TimeUnit.MILLISECONDS.toNanos(30), true);
        metrics.recordStageDuration(GitPulseMetrics.STAGE_FILE_RISK_MATERIALIZATION, TimeUnit.MILLISECONDS.toNanos(40), true);
        metrics.recordStageDuration(GitPulseMetrics.STAGE_COMMIT_CLASSIFICATION, TimeUnit.MILLISECONDS.toNanos(10), false);

        Timer commitTimer = meterRegistry.timer(
                GitPulseMetrics.METRIC_ANALYSIS_STAGE_DURATION,
                GitPulseMetrics.TAG_STAGE, GitPulseMetrics.STAGE_COMMIT_INGESTION,
                GitPulseMetrics.TAG_STATUS, GitPulseMetrics.STATUS_COMPLETED
        );
        Timer riskTimer = meterRegistry.timer(
                GitPulseMetrics.METRIC_ANALYSIS_STAGE_DURATION,
                GitPulseMetrics.TAG_STAGE, GitPulseMetrics.STAGE_FILE_RISK_MATERIALIZATION,
                GitPulseMetrics.TAG_STATUS, GitPulseMetrics.STATUS_COMPLETED
        );
        Timer failedClassifyTimer = meterRegistry.timer(
                GitPulseMetrics.METRIC_ANALYSIS_STAGE_DURATION,
                GitPulseMetrics.TAG_STAGE, GitPulseMetrics.STAGE_COMMIT_CLASSIFICATION,
                GitPulseMetrics.TAG_STATUS, GitPulseMetrics.STATUS_FAILED
        );

        assertThat(commitTimer.count()).isEqualTo(1L);
        assertThat(riskTimer.count()).isEqualTo(1L);
        assertThat(failedClassifyTimer.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should record commit and file change ingestion counts with inserted and duplicate tags")
    void testIngestionCounters() {
        metrics.recordCommitsIngested(100, 10);
        metrics.recordFileChangesIngested(250, 25);

        Counter commitInserted = meterRegistry.counter(GitPulseMetrics.METRIC_INGESTION_COMMITS, GitPulseMetrics.TAG_RESULT, GitPulseMetrics.RESULT_INSERTED);
        Counter commitDuplicates = meterRegistry.counter(GitPulseMetrics.METRIC_INGESTION_COMMITS, GitPulseMetrics.TAG_RESULT, GitPulseMetrics.RESULT_DUPLICATE);
        Counter fileInserted = meterRegistry.counter(GitPulseMetrics.METRIC_INGESTION_FILE_CHANGES, GitPulseMetrics.TAG_RESULT, GitPulseMetrics.RESULT_INSERTED);
        Counter fileDuplicates = meterRegistry.counter(GitPulseMetrics.METRIC_INGESTION_FILE_CHANGES, GitPulseMetrics.TAG_RESULT, GitPulseMetrics.RESULT_DUPLICATE);

        assertThat(commitInserted.count()).isEqualTo(100.0);
        assertThat(commitDuplicates.count()).isEqualTo(10.0);
        assertThat(fileInserted.count()).isEqualTo(250.0);
        assertThat(fileDuplicates.count()).isEqualTo(25.0);
    }

    @Test
    @DisplayName("Should record materialized records with type tags")
    void testRecordsMaterializedCounters() {
        metrics.recordRecordsMaterialized(GitPulseMetrics.TYPE_CONTRIBUTORS, 12);
        metrics.recordRecordsMaterialized(GitPulseMetrics.TYPE_REPOSITORY_FILES, 45);
        metrics.recordRecordsMaterialized(GitPulseMetrics.TYPE_CONTRIBUTOR_FILES, 60);
        metrics.recordRecordsMaterialized(GitPulseMetrics.TYPE_RISK_SCORES, 45);

        assertThat(meterRegistry.counter(GitPulseMetrics.METRIC_RECORDS_MATERIALIZED, GitPulseMetrics.TAG_TYPE, GitPulseMetrics.TYPE_CONTRIBUTORS).count())
                .isEqualTo(12.0);
        assertThat(meterRegistry.counter(GitPulseMetrics.METRIC_RECORDS_MATERIALIZED, GitPulseMetrics.TAG_TYPE, GitPulseMetrics.TYPE_REPOSITORY_FILES).count())
                .isEqualTo(45.0);
        assertThat(meterRegistry.counter(GitPulseMetrics.METRIC_RECORDS_MATERIALIZED, GitPulseMetrics.TAG_TYPE, GitPulseMetrics.TYPE_CONTRIBUTOR_FILES).count())
                .isEqualTo(60.0);
        assertThat(meterRegistry.counter(GitPulseMetrics.METRIC_RECORDS_MATERIALIZED, GitPulseMetrics.TAG_TYPE, GitPulseMetrics.TYPE_RISK_SCORES).count())
                .isEqualTo(45.0);
    }

    @Test
    @DisplayName("Should increment Kafka retries and cache errors")
    void testKafkaAndCacheMetrics() {
        metrics.incrementKafkaRetry();
        metrics.incrementKafkaRetry();

        metrics.recordCacheError(GitPulseMetrics.OPERATION_READ);
        metrics.recordCacheError(GitPulseMetrics.OPERATION_WRITE);

        assertThat(meterRegistry.counter(GitPulseMetrics.METRIC_KAFKA_RETRIES).count()).isEqualTo(2.0);
        assertThat(meterRegistry.counter(GitPulseMetrics.METRIC_CACHE_ERRORS, GitPulseMetrics.TAG_OPERATION, GitPulseMetrics.OPERATION_READ).count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter(GitPulseMetrics.METRIC_CACHE_ERRORS, GitPulseMetrics.TAG_OPERATION, GitPulseMetrics.OPERATION_WRITE).count()).isEqualTo(1.0);
    }
}
