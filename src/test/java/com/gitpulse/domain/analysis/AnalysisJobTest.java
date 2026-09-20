package com.gitpulse.domain.analysis;

import com.gitpulse.domain.repository.Repository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnalysisJobTest {

    @Test
    @DisplayName("markRunning from PENDING should transition status to RUNNING and set startedAt")
    void markRunning_FromPending_ShouldTransitionToRunning() {
        Repository repo = new Repository("owner", "name");
        AnalysisJob job = new AnalysisJob(repo, AnalysisJobStatus.PENDING);

        job.markRunning();

        assertThat(job.getStatus()).isEqualTo(AnalysisJobStatus.RUNNING);
        assertThat(job.getStartedAt()).isNotNull();
        assertThat(job.getCompletedAt()).isNull();
        assertThat(job.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("markRunning from FAILED should allow retry, transition status to RUNNING, and clear previous error state")
    void markRunning_FromFailed_ShouldAllowRetryAndClearErrors() {
        Repository repo = new Repository("owner", "name");
        AnalysisJob job = new AnalysisJob(repo, AnalysisJobStatus.RUNNING);
        job.markFailed("Initial GitHub 503 error");

        assertThat(job.getStatus()).isEqualTo(AnalysisJobStatus.FAILED);
        assertThat(job.getErrorMessage()).isEqualTo("Initial GitHub 503 error");
        assertThat(job.getCompletedAt()).isNotNull();

        // Retry transition: FAILED -> RUNNING
        job.markRunning();

        assertThat(job.getStatus()).isEqualTo(AnalysisJobStatus.RUNNING);
        assertThat(job.getStartedAt()).isNotNull();
        assertThat(job.getCompletedAt()).isNull();
        assertThat(job.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("markRunning from COMPLETED should throw IllegalStateException")
    void markRunning_FromCompleted_ShouldThrowIllegalStateException() {
        Repository repo = new Repository("owner", "name");
        AnalysisJob job = new AnalysisJob(repo, AnalysisJobStatus.RUNNING);
        job.markCompleted();

        assertThatThrownBy(job::markRunning)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot transition job");
    }

    @Test
    @DisplayName("markRunning from RUNNING should throw IllegalStateException")
    void markRunning_FromRunning_ShouldThrowIllegalStateException() {
        Repository repo = new Repository("owner", "name");
        AnalysisJob job = new AnalysisJob(repo, AnalysisJobStatus.PENDING);
        job.markRunning();

        assertThatThrownBy(job::markRunning)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot transition job");
    }

    @Test
    @DisplayName("markCompleted from RUNNING should transition status to COMPLETED and set completedAt")
    void markCompleted_FromRunning_ShouldTransitionToCompleted() {
        Repository repo = new Repository("owner", "name");
        AnalysisJob job = new AnalysisJob(repo, AnalysisJobStatus.PENDING);
        job.markRunning();
        job.markCompleted();

        assertThat(job.getStatus()).isEqualTo(AnalysisJobStatus.COMPLETED);
        assertThat(job.getCompletedAt()).isNotNull();
        assertThat(job.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("markFailed from COMPLETED should throw IllegalStateException")
    void markFailed_FromCompleted_ShouldThrowIllegalStateException() {
        Repository repo = new Repository("owner", "name");
        AnalysisJob job = new AnalysisJob(repo, AnalysisJobStatus.RUNNING);
        job.markCompleted();

        assertThatThrownBy(() -> job.markFailed("Late error"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot fail an already COMPLETED job");
    }
}
