package com.gitpulse.domain.analysis.processor;

import com.gitpulse.domain.analysis.AnalysisJob;
import com.gitpulse.domain.analysis.AnalysisJobJpaRepository;
import com.gitpulse.domain.analysis.AnalysisJobStatus;
import com.gitpulse.domain.commit.CommitIngestionService;
import com.gitpulse.domain.commit.dto.CommitIngestionResult;
import com.gitpulse.domain.repository.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryAnalysisProcessorTest {

    @Mock
    private AnalysisJobJpaRepository analysisJobJpaRepository;

    @Mock
    private CommitIngestionService commitIngestionService;

    private RepositoryAnalysisProcessor processor;
    private Repository sampleRepository;
    private AnalysisJob pendingJob;

    @BeforeEach
    void setUp() {
        processor = new RepositoryAnalysisProcessor(analysisJobJpaRepository, commitIngestionService);

        sampleRepository = new Repository("spring-projects", "spring-boot");
        ReflectionTestUtils.setField(sampleRepository, "id", 1L);

        pendingJob = new AnalysisJob(sampleRepository, AnalysisJobStatus.PENDING);
        ReflectionTestUtils.setField(pendingJob, "id", 100L);
    }

    @Test
    @DisplayName("Should successfully transition PENDING job to RUNNING, invoke CommitIngestionService, and transition to COMPLETED")
    void processJob_Success() {
        when(analysisJobJpaRepository.findWithRepositoryById(100L)).thenReturn(Optional.of(pendingJob));
        when(analysisJobJpaRepository.saveAndFlush(any(AnalysisJob.class))).thenAnswer(i -> i.getArgument(0));
        when(commitIngestionService.ingestCommits(1L, 100L))
                .thenReturn(new CommitIngestionResult(2, 50, 45, 5, 250));

        processor.processJob(100L);

        assertThat(pendingJob.getStatus()).isEqualTo(AnalysisJobStatus.COMPLETED);
        assertThat(pendingJob.getStartedAt()).isNotNull();
        assertThat(pendingJob.getCompletedAt()).isNotNull();
        assertThat(pendingJob.getErrorMessage()).isNull();

        verify(commitIngestionService).ingestCommits(1L, 100L);
        verify(analysisJobJpaRepository, times(2)).saveAndFlush(pendingJob);
    }

    @Test
    @DisplayName("Should transition job to FAILED when commit ingestion throws an exception")
    void processJob_Failure_TransitionsToFailed() {
        when(analysisJobJpaRepository.findWithRepositoryById(100L)).thenReturn(Optional.of(pendingJob));
        when(analysisJobJpaRepository.saveAndFlush(any(AnalysisJob.class))).thenAnswer(i -> i.getArgument(0));
        when(commitIngestionService.ingestCommits(1L, 100L))
                .thenThrow(new RuntimeException("GitHub API rate limit exceeded"));

        processor.processJob(100L);

        assertThat(pendingJob.getStatus()).isEqualTo(AnalysisJobStatus.FAILED);
        assertThat(pendingJob.getErrorMessage()).isEqualTo("GitHub API rate limit exceeded");
        assertThat(pendingJob.getCompletedAt()).isNotNull();

        verify(commitIngestionService).ingestCommits(1L, 100L);
        verify(analysisJobJpaRepository, times(2)).saveAndFlush(pendingJob);
    }

    @Test
    @DisplayName("Idempotency: Should ignore duplicate event when job is already COMPLETED")
    void processJob_AlreadyCompleted_Ignored() {
        AnalysisJob completedJob = new AnalysisJob(sampleRepository, AnalysisJobStatus.COMPLETED);
        ReflectionTestUtils.setField(completedJob, "id", 200L);

        when(analysisJobJpaRepository.findWithRepositoryById(200L)).thenReturn(Optional.of(completedJob));

        processor.processJob(200L);

        assertThat(completedJob.getStatus()).isEqualTo(AnalysisJobStatus.COMPLETED);
        verify(analysisJobJpaRepository, never()).saveAndFlush(any());
        verify(commitIngestionService, never()).ingestCommits(any(), any());
    }

    @Test
    @DisplayName("Idempotency: Should ignore duplicate event when job is already RUNNING")
    void processJob_AlreadyRunning_Ignored() {
        AnalysisJob runningJob = new AnalysisJob(sampleRepository, AnalysisJobStatus.RUNNING);
        ReflectionTestUtils.setField(runningJob, "id", 300L);

        when(analysisJobJpaRepository.findWithRepositoryById(300L)).thenReturn(Optional.of(runningJob));

        processor.processJob(300L);

        assertThat(runningJob.getStatus()).isEqualTo(AnalysisJobStatus.RUNNING);
        verify(analysisJobJpaRepository, never()).saveAndFlush(any());
        verify(commitIngestionService, never()).ingestCommits(any(), any());
    }

    @Test
    @DisplayName("Idempotency: Should ignore duplicate event when job is already FAILED")
    void processJob_AlreadyFailed_Ignored() {
        AnalysisJob failedJob = new AnalysisJob(sampleRepository, AnalysisJobStatus.FAILED);
        ReflectionTestUtils.setField(failedJob, "id", 400L);

        when(analysisJobJpaRepository.findWithRepositoryById(400L)).thenReturn(Optional.of(failedJob));

        processor.processJob(400L);

        assertThat(failedJob.getStatus()).isEqualTo(AnalysisJobStatus.FAILED);
        verify(analysisJobJpaRepository, never()).saveAndFlush(any());
        verify(commitIngestionService, never()).ingestCommits(any(), any());
    }

    @Test
    @DisplayName("Should safely handle nonexistent job ID")
    void processJob_NotFound_Ignored() {
        when(analysisJobJpaRepository.findWithRepositoryById(999L)).thenReturn(Optional.empty());

        processor.processJob(999L);

        verify(analysisJobJpaRepository, never()).saveAndFlush(any());
        verify(commitIngestionService, never()).ingestCommits(any(), any());
    }
}
