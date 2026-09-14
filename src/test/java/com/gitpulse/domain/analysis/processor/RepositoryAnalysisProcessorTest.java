package com.gitpulse.domain.analysis.processor;

import com.gitpulse.domain.analysis.AnalysisJob;
import com.gitpulse.domain.analysis.AnalysisJobJpaRepository;
import com.gitpulse.domain.analysis.AnalysisJobStatus;
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

    private RepositoryAnalysisProcessor processor;
    private Repository sampleRepository;
    private AnalysisJob pendingJob;

    @BeforeEach
    void setUp() {
        processor = new RepositoryAnalysisProcessor(analysisJobJpaRepository);

        sampleRepository = new Repository("spring-projects", "spring-boot");
        ReflectionTestUtils.setField(sampleRepository, "id", 1L);

        pendingJob = new AnalysisJob(sampleRepository, AnalysisJobStatus.PENDING);
        ReflectionTestUtils.setField(pendingJob, "id", 100L);
    }

    @Test
    @DisplayName("Should successfully transition PENDING job to RUNNING and then to COMPLETED")
    void processJob_Success() {
        when(analysisJobJpaRepository.findById(100L)).thenReturn(Optional.of(pendingJob));
        when(analysisJobJpaRepository.save(any(AnalysisJob.class))).thenAnswer(i -> i.getArgument(0));

        processor.processJob(100L);

        assertThat(pendingJob.getStatus()).isEqualTo(AnalysisJobStatus.COMPLETED);
        assertThat(pendingJob.getStartedAt()).isNotNull();
        assertThat(pendingJob.getCompletedAt()).isNotNull();
        assertThat(pendingJob.getErrorMessage()).isNull();

        // Saved once for RUNNING and once for COMPLETED
        verify(analysisJobJpaRepository, times(2)).save(pendingJob);
    }

    @Test
    @DisplayName("Should transition job to FAILED when an exception occurs during processing")
    void processJob_Failure_TransitionsToFailed() {
        RepositoryAnalysisProcessor failingProcessor = new RepositoryAnalysisProcessor(analysisJobJpaRepository) {
            @Override
            protected void executeAnalysis(AnalysisJob job) {
                throw new RuntimeException("Simulated GitHub analysis failure");
            }
        };

        when(analysisJobJpaRepository.findById(100L)).thenReturn(Optional.of(pendingJob));
        when(analysisJobJpaRepository.save(any(AnalysisJob.class))).thenAnswer(i -> i.getArgument(0));

        failingProcessor.processJob(100L);

        assertThat(pendingJob.getStatus()).isEqualTo(AnalysisJobStatus.FAILED);
        assertThat(pendingJob.getErrorMessage()).isEqualTo("Simulated GitHub analysis failure");
        assertThat(pendingJob.getCompletedAt()).isNotNull();

        verify(analysisJobJpaRepository, times(2)).save(pendingJob);
    }

    @Test
    @DisplayName("Idempotency: Should ignore duplicate event when job is already COMPLETED")
    void processJob_AlreadyCompleted_Ignored() {
        AnalysisJob completedJob = new AnalysisJob(sampleRepository, AnalysisJobStatus.COMPLETED);
        ReflectionTestUtils.setField(completedJob, "id", 200L);

        when(analysisJobJpaRepository.findById(200L)).thenReturn(Optional.of(completedJob));

        processor.processJob(200L);

        assertThat(completedJob.getStatus()).isEqualTo(AnalysisJobStatus.COMPLETED);
        verify(analysisJobJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Idempotency: Should ignore duplicate event when job is already RUNNING")
    void processJob_AlreadyRunning_Ignored() {
        AnalysisJob runningJob = new AnalysisJob(sampleRepository, AnalysisJobStatus.RUNNING);
        ReflectionTestUtils.setField(runningJob, "id", 300L);

        when(analysisJobJpaRepository.findById(300L)).thenReturn(Optional.of(runningJob));

        processor.processJob(300L);

        assertThat(runningJob.getStatus()).isEqualTo(AnalysisJobStatus.RUNNING);
        verify(analysisJobJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Idempotency: Should ignore duplicate event when job is already FAILED")
    void processJob_AlreadyFailed_Ignored() {
        AnalysisJob failedJob = new AnalysisJob(sampleRepository, AnalysisJobStatus.FAILED);
        ReflectionTestUtils.setField(failedJob, "id", 400L);

        when(analysisJobJpaRepository.findById(400L)).thenReturn(Optional.of(failedJob));

        processor.processJob(400L);

        assertThat(failedJob.getStatus()).isEqualTo(AnalysisJobStatus.FAILED);
        verify(analysisJobJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should safely handle nonexistent job ID")
    void processJob_NotFound_Ignored() {
        when(analysisJobJpaRepository.findById(999L)).thenReturn(Optional.empty());

        processor.processJob(999L);

        verify(analysisJobJpaRepository, never()).save(any());
    }
}
