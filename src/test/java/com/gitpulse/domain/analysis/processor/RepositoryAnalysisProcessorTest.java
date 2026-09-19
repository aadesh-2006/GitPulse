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
import com.gitpulse.domain.file.RepositoryFileAggregationService;
import com.gitpulse.domain.file.dto.RepositoryFileAggregationResult;
import com.gitpulse.domain.filechange.FileChangeIngestionService;
import com.gitpulse.domain.filechange.dto.FileChangeIngestionResult;
import com.gitpulse.domain.repository.Repository;
import com.gitpulse.domain.risk.RepositoryFileRiskMaterializationService;
import com.gitpulse.domain.risk.dto.RepositoryFileRiskMaterializationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
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

    @Mock
    private CommitClassificationPipelineService commitClassificationPipelineService;

    @Mock
    private FileChangeIngestionService fileChangeIngestionService;

    @Mock
    private ContributorAggregationService contributorAggregationService;

    @Mock
    private RepositoryFileAggregationService repositoryFileAggregationService;

    @Mock
    private RepositoryContributorFileAggregationService repositoryContributorFileAggregationService;

    @Mock
    private RepositoryFileRiskMaterializationService repositoryFileRiskMaterializationService;

    private RepositoryAnalysisProcessor processor;
    private Repository sampleRepository;
    private AnalysisJob pendingJob;
    private Instant fixedJobCreatedAt;

    @BeforeEach
    void setUp() {
        processor = new RepositoryAnalysisProcessor(
                analysisJobJpaRepository,
                commitIngestionService,
                commitClassificationPipelineService,
                fileChangeIngestionService,
                contributorAggregationService,
                repositoryFileAggregationService,
                repositoryContributorFileAggregationService,
                repositoryFileRiskMaterializationService
        );

        sampleRepository = new Repository("spring-projects", "spring-boot");
        ReflectionTestUtils.setField(sampleRepository, "id", 1L);

        fixedJobCreatedAt = Instant.parse("2026-03-15T10:00:00Z");
        pendingJob = new AnalysisJob(sampleRepository, AnalysisJobStatus.PENDING);
        ReflectionTestUtils.setField(pendingJob, "id", 100L);
        ReflectionTestUtils.setField(pendingJob, "createdAt", fixedJobCreatedAt);
    }

    @Test
    @DisplayName("Should successfully transition PENDING job to RUNNING, invoke all 7 stages in strict dependency order, and transition to COMPLETED")
    void processJob_Success() {
        when(analysisJobJpaRepository.findWithRepositoryById(100L)).thenReturn(Optional.of(pendingJob));
        when(analysisJobJpaRepository.saveAndFlush(any(AnalysisJob.class))).thenAnswer(i -> i.getArgument(0));
        when(commitIngestionService.ingestCommits(1L, 100L))
                .thenReturn(new CommitIngestionResult(2, 50, 45, 5, 250));
        when(commitClassificationPipelineService.classifyCommits(1L, 100L))
                .thenReturn(new CommitClassificationResult(45, 45, 100));
        when(fileChangeIngestionService.ingestFileChanges(1L, 100L))
                .thenReturn(new FileChangeIngestionResult(45, 0, 120, 120, 0, 300));
        when(contributorAggregationService.aggregateContributors(1L, 100L))
                .thenReturn(new ContributorAggregationResult(5, 5, 5, 0, 50));
        when(repositoryFileAggregationService.aggregateRepositoryFiles(1L))
                .thenReturn(new RepositoryFileAggregationResult(1L, 10, 8, 2, 0));
        when(repositoryContributorFileAggregationService.aggregateRepositoryContributorFiles(1L))
                .thenReturn(new RepositoryContributorFileAggregationResult(1L, 10, 8, 2, 0, 0));
        when(repositoryFileRiskMaterializationService.materializeFileRisks(eq(1L), eq(fixedJobCreatedAt)))
                .thenReturn(new RepositoryFileRiskMaterializationResult(1L, 10, 8, 2));

        processor.processJob(100L);

        assertThat(pendingJob.getStatus()).isEqualTo(AnalysisJobStatus.COMPLETED);
        assertThat(pendingJob.getStartedAt()).isNotNull();
        assertThat(pendingJob.getCompletedAt()).isNotNull();
        assertThat(pendingJob.getErrorMessage()).isNull();

        InOrder inOrder = inOrder(
                commitIngestionService,
                commitClassificationPipelineService,
                fileChangeIngestionService,
                contributorAggregationService,
                repositoryFileAggregationService,
                repositoryContributorFileAggregationService,
                repositoryFileRiskMaterializationService
        );

        inOrder.verify(commitIngestionService).ingestCommits(1L, 100L);
        inOrder.verify(commitClassificationPipelineService).classifyCommits(1L, 100L);
        inOrder.verify(fileChangeIngestionService).ingestFileChanges(1L, 100L);
        inOrder.verify(contributorAggregationService).aggregateContributors(1L, 100L);
        inOrder.verify(repositoryFileAggregationService).aggregateRepositoryFiles(1L);
        inOrder.verify(repositoryContributorFileAggregationService).aggregateRepositoryContributorFiles(1L);
        inOrder.verify(repositoryFileRiskMaterializationService).materializeFileRisks(1L, fixedJobCreatedAt);

        verify(analysisJobJpaRepository, times(2)).saveAndFlush(pendingJob);
    }

    @Test
    @DisplayName("Should pass exact job.getCreatedAt() as referenceTime to risk materialization service")
    void processJob_PassesJobCreatedAtAsReferenceTimeToRiskMaterialization() {
        Instant expectedCreatedAt = Instant.parse("2025-06-01T12:30:45Z");
        ReflectionTestUtils.setField(pendingJob, "createdAt", expectedCreatedAt);

        when(analysisJobJpaRepository.findWithRepositoryById(100L)).thenReturn(Optional.of(pendingJob));
        when(analysisJobJpaRepository.saveAndFlush(any(AnalysisJob.class))).thenAnswer(i -> i.getArgument(0));
        when(commitIngestionService.ingestCommits(1L, 100L))
                .thenReturn(new CommitIngestionResult(1, 1, 1, 0, 10));
        when(commitClassificationPipelineService.classifyCommits(1L, 100L))
                .thenReturn(new CommitClassificationResult(1, 1, 10));
        when(fileChangeIngestionService.ingestFileChanges(1L, 100L))
                .thenReturn(new FileChangeIngestionResult(1, 0, 1, 1, 0, 10));
        when(contributorAggregationService.aggregateContributors(1L, 100L))
                .thenReturn(new ContributorAggregationResult(1, 1, 1, 0, 10));
        when(repositoryFileAggregationService.aggregateRepositoryFiles(1L))
                .thenReturn(new RepositoryFileAggregationResult(1L, 1, 1, 0, 0));
        when(repositoryContributorFileAggregationService.aggregateRepositoryContributorFiles(1L))
                .thenReturn(new RepositoryContributorFileAggregationResult(1L, 1, 1, 0, 0, 0));
        when(repositoryFileRiskMaterializationService.materializeFileRisks(eq(1L), any(Instant.class)))
                .thenReturn(new RepositoryFileRiskMaterializationResult(1L, 1, 1, 0));

        processor.processJob(100L);

        ArgumentCaptor<Instant> referenceTimeCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(repositoryFileRiskMaterializationService).materializeFileRisks(eq(1L), referenceTimeCaptor.capture());

        assertThat(referenceTimeCaptor.getValue()).isEqualTo(expectedCreatedAt);
    }

    @Test
    @DisplayName("Should mark job FAILED and rethrow NullPointerException when job.getCreatedAt() is unexpectedly null")
    void processJob_Failure_NullCreatedAt_TransitionsToFailedAndRethrows() {
        ReflectionTestUtils.setField(pendingJob, "createdAt", null);

        when(analysisJobJpaRepository.findWithRepositoryById(100L)).thenReturn(Optional.of(pendingJob));
        when(analysisJobJpaRepository.saveAndFlush(any(AnalysisJob.class))).thenAnswer(i -> i.getArgument(0));
        when(commitIngestionService.ingestCommits(1L, 100L))
                .thenReturn(new CommitIngestionResult(1, 1, 1, 0, 10));
        when(commitClassificationPipelineService.classifyCommits(1L, 100L))
                .thenReturn(new CommitClassificationResult(1, 1, 10));
        when(fileChangeIngestionService.ingestFileChanges(1L, 100L))
                .thenReturn(new FileChangeIngestionResult(1, 0, 1, 1, 0, 10));
        when(contributorAggregationService.aggregateContributors(1L, 100L))
                .thenReturn(new ContributorAggregationResult(1, 1, 1, 0, 10));
        when(repositoryFileAggregationService.aggregateRepositoryFiles(1L))
                .thenReturn(new RepositoryFileAggregationResult(1L, 1, 1, 0, 0));
        when(repositoryContributorFileAggregationService.aggregateRepositoryContributorFiles(1L))
                .thenReturn(new RepositoryContributorFileAggregationResult(1L, 1, 1, 0, 0, 0));

        assertThatThrownBy(() -> processor.processJob(100L))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Analysis job createdAt must not be null before processing");

        assertThat(pendingJob.getStatus()).isEqualTo(AnalysisJobStatus.FAILED);
        assertThat(pendingJob.getErrorMessage()).isEqualTo("Analysis job createdAt must not be null before processing");
        assertThat(pendingJob.getCompletedAt()).isNotNull();

        verify(repositoryFileRiskMaterializationService, never()).materializeFileRisks(any(), any());
        verify(analysisJobJpaRepository, times(2)).saveAndFlush(pendingJob);
    }

    @Test
    @DisplayName("Should mark job FAILED and rethrow exception when commit ingestion fails")
    void processJob_Failure_CommitIngestionThrows_TransitionsToFailedAndRethrows() {
        when(analysisJobJpaRepository.findWithRepositoryById(100L)).thenReturn(Optional.of(pendingJob));
        when(analysisJobJpaRepository.saveAndFlush(any(AnalysisJob.class))).thenAnswer(i -> i.getArgument(0));
        when(commitIngestionService.ingestCommits(1L, 100L))
                .thenThrow(new RuntimeException("GitHub API rate limit exceeded"));

        assertThatThrownBy(() -> processor.processJob(100L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("GitHub API rate limit exceeded");

        assertThat(pendingJob.getStatus()).isEqualTo(AnalysisJobStatus.FAILED);
        assertThat(pendingJob.getErrorMessage()).isEqualTo("GitHub API rate limit exceeded");
        assertThat(pendingJob.getCompletedAt()).isNotNull();

        verify(commitIngestionService).ingestCommits(1L, 100L);
        verify(commitClassificationPipelineService, never()).classifyCommits(any(), any());
        verify(fileChangeIngestionService, never()).ingestFileChanges(any(), any());
        verify(contributorAggregationService, never()).aggregateContributors(any(), any());
        verify(repositoryFileAggregationService, never()).aggregateRepositoryFiles(any());
        verify(repositoryContributorFileAggregationService, never()).aggregateRepositoryContributorFiles(any());
        verify(repositoryFileRiskMaterializationService, never()).materializeFileRisks(any(), any());
        verify(analysisJobJpaRepository, times(2)).saveAndFlush(pendingJob);
    }

    @Test
    @DisplayName("Should mark job FAILED and rethrow exception when commit classification fails")
    void processJob_Failure_CommitClassificationThrows_TransitionsToFailedAndRethrows() {
        when(analysisJobJpaRepository.findWithRepositoryById(100L)).thenReturn(Optional.of(pendingJob));
        when(analysisJobJpaRepository.saveAndFlush(any(AnalysisJob.class))).thenAnswer(i -> i.getArgument(0));
        when(commitIngestionService.ingestCommits(1L, 100L))
                .thenReturn(new CommitIngestionResult(1, 10, 10, 0, 100));
        when(commitClassificationPipelineService.classifyCommits(1L, 100L))
                .thenThrow(new RuntimeException("Classification DB write failure"));

        assertThatThrownBy(() -> processor.processJob(100L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Classification DB write failure");

        assertThat(pendingJob.getStatus()).isEqualTo(AnalysisJobStatus.FAILED);
        assertThat(pendingJob.getErrorMessage()).isEqualTo("Classification DB write failure");
        assertThat(pendingJob.getCompletedAt()).isNotNull();

        verify(commitIngestionService).ingestCommits(1L, 100L);
        verify(commitClassificationPipelineService).classifyCommits(1L, 100L);
        verify(fileChangeIngestionService, never()).ingestFileChanges(any(), any());
        verify(contributorAggregationService, never()).aggregateContributors(any(), any());
        verify(repositoryFileAggregationService, never()).aggregateRepositoryFiles(any());
        verify(repositoryContributorFileAggregationService, never()).aggregateRepositoryContributorFiles(any());
        verify(repositoryFileRiskMaterializationService, never()).materializeFileRisks(any(), any());
        verify(analysisJobJpaRepository, times(2)).saveAndFlush(pendingJob);
    }

    @Test
    @DisplayName("Should mark job FAILED and rethrow exception when file-change ingestion fails")
    void processJob_Failure_FileChangeThrows_TransitionsToFailedAndRethrows() {
        when(analysisJobJpaRepository.findWithRepositoryById(100L)).thenReturn(Optional.of(pendingJob));
        when(analysisJobJpaRepository.saveAndFlush(any(AnalysisJob.class))).thenAnswer(i -> i.getArgument(0));
        when(commitIngestionService.ingestCommits(1L, 100L))
                .thenReturn(new CommitIngestionResult(1, 10, 10, 0, 100));
        when(commitClassificationPipelineService.classifyCommits(1L, 100L))
                .thenReturn(new CommitClassificationResult(10, 10, 50));
        when(fileChangeIngestionService.ingestFileChanges(1L, 100L))
                .thenThrow(new RuntimeException("GitHub 403 Rate Limit on commit detail"));

        assertThatThrownBy(() -> processor.processJob(100L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("GitHub 403 Rate Limit on commit detail");

        assertThat(pendingJob.getStatus()).isEqualTo(AnalysisJobStatus.FAILED);
        assertThat(pendingJob.getErrorMessage()).isEqualTo("GitHub 403 Rate Limit on commit detail");
        assertThat(pendingJob.getCompletedAt()).isNotNull();

        verify(commitIngestionService).ingestCommits(1L, 100L);
        verify(commitClassificationPipelineService).classifyCommits(1L, 100L);
        verify(fileChangeIngestionService).ingestFileChanges(1L, 100L);
        verify(contributorAggregationService, never()).aggregateContributors(any(), any());
        verify(repositoryFileAggregationService, never()).aggregateRepositoryFiles(any());
        verify(repositoryContributorFileAggregationService, never()).aggregateRepositoryContributorFiles(any());
        verify(repositoryFileRiskMaterializationService, never()).materializeFileRisks(any(), any());
        verify(analysisJobJpaRepository, times(2)).saveAndFlush(pendingJob);
    }

    @Test
    @DisplayName("Should mark job FAILED and rethrow exception when contributor aggregation fails")
    void processJob_Failure_ContributorAggregationThrows_TransitionsToFailedAndRethrows() {
        when(analysisJobJpaRepository.findWithRepositoryById(100L)).thenReturn(Optional.of(pendingJob));
        when(analysisJobJpaRepository.saveAndFlush(any(AnalysisJob.class))).thenAnswer(i -> i.getArgument(0));
        when(commitIngestionService.ingestCommits(1L, 100L))
                .thenReturn(new CommitIngestionResult(1, 10, 10, 0, 100));
        when(commitClassificationPipelineService.classifyCommits(1L, 100L))
                .thenReturn(new CommitClassificationResult(10, 10, 50));
        when(fileChangeIngestionService.ingestFileChanges(1L, 100L))
                .thenReturn(new FileChangeIngestionResult(10, 0, 20, 20, 0, 100));
        when(contributorAggregationService.aggregateContributors(1L, 100L))
                .thenThrow(new RuntimeException("Database error during contributor aggregation"));

        assertThatThrownBy(() -> processor.processJob(100L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error during contributor aggregation");

        assertThat(pendingJob.getStatus()).isEqualTo(AnalysisJobStatus.FAILED);
        assertThat(pendingJob.getErrorMessage()).isEqualTo("Database error during contributor aggregation");
        assertThat(pendingJob.getCompletedAt()).isNotNull();

        verify(commitIngestionService).ingestCommits(1L, 100L);
        verify(commitClassificationPipelineService).classifyCommits(1L, 100L);
        verify(fileChangeIngestionService).ingestFileChanges(1L, 100L);
        verify(contributorAggregationService).aggregateContributors(1L, 100L);
        verify(repositoryFileAggregationService, never()).aggregateRepositoryFiles(any());
        verify(repositoryContributorFileAggregationService, never()).aggregateRepositoryContributorFiles(any());
        verify(repositoryFileRiskMaterializationService, never()).materializeFileRisks(any(), any());
        verify(analysisJobJpaRepository, times(2)).saveAndFlush(pendingJob);
    }

    @Test
    @DisplayName("Should mark job FAILED and rethrow exception when repository file aggregation fails")
    void processJob_Failure_RepositoryFileAggregationThrows_TransitionsToFailedAndRethrows() {
        when(analysisJobJpaRepository.findWithRepositoryById(100L)).thenReturn(Optional.of(pendingJob));
        when(analysisJobJpaRepository.saveAndFlush(any(AnalysisJob.class))).thenAnswer(i -> i.getArgument(0));
        when(commitIngestionService.ingestCommits(1L, 100L))
                .thenReturn(new CommitIngestionResult(1, 10, 10, 0, 100));
        when(commitClassificationPipelineService.classifyCommits(1L, 100L))
                .thenReturn(new CommitClassificationResult(10, 10, 50));
        when(fileChangeIngestionService.ingestFileChanges(1L, 100L))
                .thenReturn(new FileChangeIngestionResult(10, 0, 20, 20, 0, 100));
        when(contributorAggregationService.aggregateContributors(1L, 100L))
                .thenReturn(new ContributorAggregationResult(5, 5, 5, 0, 50));
        when(repositoryFileAggregationService.aggregateRepositoryFiles(1L))
                .thenThrow(new RuntimeException("Database error during file aggregation"));

        assertThatThrownBy(() -> processor.processJob(100L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error during file aggregation");

        assertThat(pendingJob.getStatus()).isEqualTo(AnalysisJobStatus.FAILED);
        assertThat(pendingJob.getErrorMessage()).isEqualTo("Database error during file aggregation");
        assertThat(pendingJob.getCompletedAt()).isNotNull();

        verify(commitIngestionService).ingestCommits(1L, 100L);
        verify(commitClassificationPipelineService).classifyCommits(1L, 100L);
        verify(fileChangeIngestionService).ingestFileChanges(1L, 100L);
        verify(contributorAggregationService).aggregateContributors(1L, 100L);
        verify(repositoryFileAggregationService).aggregateRepositoryFiles(1L);
        verify(repositoryContributorFileAggregationService, never()).aggregateRepositoryContributorFiles(any());
        verify(repositoryFileRiskMaterializationService, never()).materializeFileRisks(any(), any());
        verify(analysisJobJpaRepository, times(2)).saveAndFlush(pendingJob);
    }

    @Test
    @DisplayName("Should mark job FAILED and rethrow exception when contributor-file aggregation fails")
    void processJob_Failure_ContributorFileAggregationThrows_TransitionsToFailedAndRethrows() {
        when(analysisJobJpaRepository.findWithRepositoryById(100L)).thenReturn(Optional.of(pendingJob));
        when(analysisJobJpaRepository.saveAndFlush(any(AnalysisJob.class))).thenAnswer(i -> i.getArgument(0));
        when(commitIngestionService.ingestCommits(1L, 100L))
                .thenReturn(new CommitIngestionResult(1, 10, 10, 0, 100));
        when(commitClassificationPipelineService.classifyCommits(1L, 100L))
                .thenReturn(new CommitClassificationResult(10, 10, 50));
        when(fileChangeIngestionService.ingestFileChanges(1L, 100L))
                .thenReturn(new FileChangeIngestionResult(10, 0, 20, 20, 0, 100));
        when(contributorAggregationService.aggregateContributors(1L, 100L))
                .thenReturn(new ContributorAggregationResult(5, 5, 5, 0, 50));
        when(repositoryFileAggregationService.aggregateRepositoryFiles(1L))
                .thenReturn(new RepositoryFileAggregationResult(1L, 10, 8, 2, 0));
        when(repositoryContributorFileAggregationService.aggregateRepositoryContributorFiles(1L))
                .thenThrow(new RuntimeException("Database deadlock during contributor-file aggregation"));

        assertThatThrownBy(() -> processor.processJob(100L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database deadlock during contributor-file aggregation");

        assertThat(pendingJob.getStatus()).isEqualTo(AnalysisJobStatus.FAILED);
        assertThat(pendingJob.getErrorMessage()).isEqualTo("Database deadlock during contributor-file aggregation");
        assertThat(pendingJob.getCompletedAt()).isNotNull();

        verify(commitIngestionService).ingestCommits(1L, 100L);
        verify(commitClassificationPipelineService).classifyCommits(1L, 100L);
        verify(fileChangeIngestionService).ingestFileChanges(1L, 100L);
        verify(contributorAggregationService).aggregateContributors(1L, 100L);
        verify(repositoryFileAggregationService).aggregateRepositoryFiles(1L);
        verify(repositoryContributorFileAggregationService).aggregateRepositoryContributorFiles(1L);
        verify(repositoryFileRiskMaterializationService, never()).materializeFileRisks(any(), any());
        verify(analysisJobJpaRepository, times(2)).saveAndFlush(pendingJob);
    }

    @Test
    @DisplayName("Should mark job FAILED and rethrow exception when file risk materialization fails")
    void processJob_Failure_RiskMaterializationThrows_TransitionsToFailedAndRethrows() {
        when(analysisJobJpaRepository.findWithRepositoryById(100L)).thenReturn(Optional.of(pendingJob));
        when(analysisJobJpaRepository.saveAndFlush(any(AnalysisJob.class))).thenAnswer(i -> i.getArgument(0));
        when(commitIngestionService.ingestCommits(1L, 100L))
                .thenReturn(new CommitIngestionResult(1, 10, 10, 0, 100));
        when(commitClassificationPipelineService.classifyCommits(1L, 100L))
                .thenReturn(new CommitClassificationResult(10, 10, 50));
        when(fileChangeIngestionService.ingestFileChanges(1L, 100L))
                .thenReturn(new FileChangeIngestionResult(10, 0, 20, 20, 0, 100));
        when(contributorAggregationService.aggregateContributors(1L, 100L))
                .thenReturn(new ContributorAggregationResult(5, 5, 5, 0, 50));
        when(repositoryFileAggregationService.aggregateRepositoryFiles(1L))
                .thenReturn(new RepositoryFileAggregationResult(1L, 10, 8, 2, 0));
        when(repositoryContributorFileAggregationService.aggregateRepositoryContributorFiles(1L))
                .thenReturn(new RepositoryContributorFileAggregationResult(1L, 10, 8, 2, 0, 0));
        when(repositoryFileRiskMaterializationService.materializeFileRisks(eq(1L), eq(fixedJobCreatedAt)))
                .thenThrow(new RuntimeException("Failure during file risk score materialization"));

        assertThatThrownBy(() -> processor.processJob(100L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failure during file risk score materialization");

        assertThat(pendingJob.getStatus()).isEqualTo(AnalysisJobStatus.FAILED);
        assertThat(pendingJob.getErrorMessage()).isEqualTo("Failure during file risk score materialization");
        assertThat(pendingJob.getCompletedAt()).isNotNull();

        verify(commitIngestionService).ingestCommits(1L, 100L);
        verify(commitClassificationPipelineService).classifyCommits(1L, 100L);
        verify(fileChangeIngestionService).ingestFileChanges(1L, 100L);
        verify(contributorAggregationService).aggregateContributors(1L, 100L);
        verify(repositoryFileAggregationService).aggregateRepositoryFiles(1L);
        verify(repositoryContributorFileAggregationService).aggregateRepositoryContributorFiles(1L);
        verify(repositoryFileRiskMaterializationService).materializeFileRisks(1L, fixedJobCreatedAt);
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
        verify(repositoryFileAggregationService, never()).aggregateRepositoryFiles(any());
        verify(repositoryContributorFileAggregationService, never()).aggregateRepositoryContributorFiles(any());
        verify(repositoryFileRiskMaterializationService, never()).materializeFileRisks(any(), any());
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
        verify(repositoryFileAggregationService, never()).aggregateRepositoryFiles(any());
        verify(repositoryContributorFileAggregationService, never()).aggregateRepositoryContributorFiles(any());
        verify(repositoryFileRiskMaterializationService, never()).materializeFileRisks(any(), any());
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
        verify(repositoryFileAggregationService, never()).aggregateRepositoryFiles(any());
        verify(repositoryContributorFileAggregationService, never()).aggregateRepositoryContributorFiles(any());
        verify(repositoryFileRiskMaterializationService, never()).materializeFileRisks(any(), any());
    }

    @Test
    @DisplayName("Should safely handle nonexistent job ID")
    void processJob_NotFound_Ignored() {
        when(analysisJobJpaRepository.findWithRepositoryById(999L)).thenReturn(Optional.empty());

        processor.processJob(999L);

        verify(analysisJobJpaRepository, never()).saveAndFlush(any());
        verify(commitIngestionService, never()).ingestCommits(any(), any());
        verify(repositoryFileAggregationService, never()).aggregateRepositoryFiles(any());
        verify(repositoryContributorFileAggregationService, never()).aggregateRepositoryContributorFiles(any());
        verify(repositoryFileRiskMaterializationService, never()).materializeFileRisks(any(), any());
    }
}