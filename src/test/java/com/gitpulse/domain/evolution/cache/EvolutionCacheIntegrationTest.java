package com.gitpulse.domain.evolution.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitpulse.config.cache.EvolutionCacheKeyGenerator;
import com.gitpulse.config.cache.RedisCacheConfig;
import com.gitpulse.domain.analysis.AnalysisJob;
import com.gitpulse.domain.analysis.AnalysisJobJpaRepository;
import com.gitpulse.domain.analysis.AnalysisJobStatus;
import com.gitpulse.domain.analysis.processor.RepositoryAnalysisProcessor;
import com.gitpulse.domain.commit.CommitClassificationPipelineService;
import com.gitpulse.domain.commit.CommitIngestionService;
import com.gitpulse.domain.commit.dto.CommitClassificationResult;
import com.gitpulse.domain.commit.dto.CommitIngestionResult;
import com.gitpulse.domain.contributor.ContributorAggregationService;
import com.gitpulse.domain.contributor.dto.ContributorAggregationResult;
import com.gitpulse.domain.contributorfile.RepositoryContributorFileAggregationService;
import com.gitpulse.domain.contributorfile.dto.RepositoryContributorFileAggregationResult;
import com.gitpulse.domain.evolution.RepositoryEvolutionComparisonQueryService;
import com.gitpulse.domain.evolution.RepositoryEvolutionCompositionQueryService;
import com.gitpulse.domain.evolution.RepositoryEvolutionJpaRepository;
import com.gitpulse.domain.evolution.RepositoryEvolutionQueryService;
import com.gitpulse.domain.evolution.dto.RepositoryEvolutionComparisonResponse;
import com.gitpulse.domain.evolution.dto.RepositoryEvolutionCompositionResponse;
import com.gitpulse.domain.evolution.dto.RepositoryEvolutionPeriodSummaryRow;
import com.gitpulse.domain.evolution.dto.RepositoryEvolutionResponse;
import com.gitpulse.domain.evolution.dto.RepositoryMonthlyEvolutionBucketResponse;
import com.gitpulse.domain.file.RepositoryFileAggregationService;
import com.gitpulse.domain.file.dto.RepositoryFileAggregationResult;
import com.gitpulse.domain.filechange.FileChangeIngestionService;
import com.gitpulse.domain.filechange.dto.FileChangeIngestionResult;
import com.gitpulse.domain.repository.Repository;
import com.gitpulse.domain.repository.RepositoryJpaRepository;
import com.gitpulse.domain.risk.RepositoryFileRiskMaterializationService;
import com.gitpulse.domain.risk.dto.RepositoryFileRiskMaterializationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
        RepositoryEvolutionQueryService.class,
        RepositoryEvolutionComparisonQueryService.class,
        RepositoryEvolutionCompositionQueryService.class,
        EvolutionCacheKeyGenerator.class,
        EvolutionCacheIntegrationTest.TestCacheConfig.class
})
@ActiveProfiles("test")
class EvolutionCacheIntegrationTest {

    @TestConfiguration
    @EnableCaching
    @Import(RedisCacheConfig.class)
    static class TestCacheConfig {
        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                    RedisCacheConfig.CACHE_EVOLUTION,
                    RedisCacheConfig.CACHE_EVOLUTION_COMPARISON,
                    RedisCacheConfig.CACHE_EVOLUTION_COMPOSITION
            );
        }
    }

    @MockBean
    private RepositoryJpaRepository repositoryJpaRepository;

    @MockBean
    private RepositoryEvolutionJpaRepository evolutionJpaRepository;

    @MockBean
    private RepositoryEvolutionCacheVersionService cacheVersionService;

    @Autowired
    private RepositoryEvolutionQueryService evolutionQueryService;

    @Autowired
    private RepositoryEvolutionComparisonQueryService comparisonQueryService;

    @Autowired
    private RepositoryEvolutionCompositionQueryService compositionQueryService;

    @Autowired
    private CacheManager cacheManager;

    private final ObjectMapper redisObjectMapper = new RedisCacheConfig().createRedisObjectMapper();

    private RepositoryEvolutionPeriodSummaryRow createMockSummaryRow() {
        RepositoryEvolutionPeriodSummaryRow row = mock(RepositoryEvolutionPeriodSummaryRow.class);
        when(row.getTotalCommits()).thenReturn(10L);
        when(row.getTotalAdditions()).thenReturn(100L);
        when(row.getTotalDeletions()).thenReturn(50L);
        when(row.getTotalChurn()).thenReturn(150L);
        when(row.getActiveContributors()).thenReturn(3L);
        when(row.getFilesChanged()).thenReturn(5L);
        return row;
    }

    @Test
    @DisplayName("Evolution query service should cache result and avoid duplicate DB hits")
    void getRepositoryEvolution_ShouldBeCached() {
        Long repoId = 1L;
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-03-01T00:00:00Z");

        when(repositoryJpaRepository.existsById(repoId)).thenReturn(true);
        when(cacheVersionService.getCurrentVersion(repoId)).thenReturn(1L);
        when(evolutionJpaRepository.findMonthlyEvolution(eq(repoId), eq(from), eq(to))).thenReturn(List.of());

        // First invocation - should query DB
        RepositoryEvolutionResponse res1 = evolutionQueryService.getRepositoryEvolution(repoId, from, to);
        assertThat(res1).isNotNull();
        verify(evolutionJpaRepository, times(1)).findMonthlyEvolution(repoId, from, to);

        // Second invocation with same args - should be served from cache
        RepositoryEvolutionResponse res2 = evolutionQueryService.getRepositoryEvolution(repoId, from, to);
        assertThat(res2).isNotNull();
        verify(evolutionJpaRepository, times(1)).findMonthlyEvolution(repoId, from, to);
    }

    @Test
    @DisplayName("Evolution comparison query service should cache result and avoid duplicate DB hits")
    void compareRepositoryEvolution_ShouldBeCached() {
        Long repoId = 2L;
        Instant currentFrom = Instant.parse("2026-02-01T00:00:00Z");
        Instant currentTo = Instant.parse("2026-03-01T00:00:00Z");
        Instant previousFrom = Instant.parse("2026-01-01T00:00:00Z");
        Instant previousTo = Instant.parse("2026-02-01T00:00:00Z");

        RepositoryEvolutionPeriodSummaryRow mockRow = createMockSummaryRow();

        when(repositoryJpaRepository.existsById(repoId)).thenReturn(true);
        when(cacheVersionService.getCurrentVersion(repoId)).thenReturn(1L);
        when(evolutionJpaRepository.findPeriodSummary(eq(repoId), any(), any())).thenReturn(mockRow);

        // First invocation
        RepositoryEvolutionComparisonResponse res1 = comparisonQueryService.compareRepositoryEvolution(
                repoId, currentFrom, currentTo, previousFrom, previousTo);
        assertThat(res1).isNotNull();
        verify(evolutionJpaRepository, times(2)).findPeriodSummary(eq(repoId), any(), any());

        // Second invocation - cached
        RepositoryEvolutionComparisonResponse res2 = comparisonQueryService.compareRepositoryEvolution(
                repoId, currentFrom, currentTo, previousFrom, previousTo);
        assertThat(res2).isNotNull();
        verify(evolutionJpaRepository, times(2)).findPeriodSummary(eq(repoId), any(), any());
    }

    @Test
    @DisplayName("Evolution composition query service should cache result and avoid duplicate DB hits")
    void getEvolutionComposition_ShouldBeCached() {
        Long repoId = 3L;
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-03-01T00:00:00Z");

        RepositoryEvolutionPeriodSummaryRow mockRow = createMockSummaryRow();

        when(repositoryJpaRepository.existsById(repoId)).thenReturn(true);
        when(cacheVersionService.getCurrentVersion(repoId)).thenReturn(1L);
        when(evolutionJpaRepository.findPeriodSummary(eq(repoId), eq(from), eq(to))).thenReturn(mockRow);

        // First invocation
        RepositoryEvolutionCompositionResponse res1 = compositionQueryService.getEvolutionComposition(repoId, from, to);
        assertThat(res1).isNotNull();
        verify(evolutionJpaRepository, times(1)).findPeriodSummary(repoId, from, to);

        // Second invocation - cached
        RepositoryEvolutionCompositionResponse res2 = compositionQueryService.getEvolutionComposition(repoId, from, to);
        assertThat(res2).isNotNull();
        verify(evolutionJpaRepository, times(1)).findPeriodSummary(repoId, from, to);
    }

    @Test
    @DisplayName("Version increment should cause cache miss and fetch fresh data")
    void versionIncrement_ShouldCauseCacheMiss() {
        Long repoId = 4L;
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-03-01T00:00:00Z");

        when(repositoryJpaRepository.existsById(repoId)).thenReturn(true);
        when(cacheVersionService.getCurrentVersion(repoId)).thenReturn(1L);
        when(evolutionJpaRepository.findMonthlyEvolution(eq(repoId), eq(from), eq(to))).thenReturn(List.of());

        // First invocation with version 1
        evolutionQueryService.getRepositoryEvolution(repoId, from, to);
        verify(evolutionJpaRepository, times(1)).findMonthlyEvolution(repoId, from, to);

        // Invalidate via version increment
        when(cacheVersionService.getCurrentVersion(repoId)).thenReturn(2L);

        // Subsequent invocation should miss cache due to key change
        evolutionQueryService.getRepositoryEvolution(repoId, from, to);
        verify(evolutionJpaRepository, times(2)).findMonthlyEvolution(repoId, from, to);
    }

    @Test
    @DisplayName("Analysis processor should increment cache version on successful completion")
    void analysisProcessor_OnSuccess_ShouldIncrementCacheVersion() {
        AnalysisJobJpaRepository jobRepo = mock(AnalysisJobJpaRepository.class);
        CommitIngestionService commitService = mock(CommitIngestionService.class);
        CommitClassificationPipelineService classificationService = mock(CommitClassificationPipelineService.class);
        FileChangeIngestionService fileChangeService = mock(FileChangeIngestionService.class);
        ContributorAggregationService contributorService = mock(ContributorAggregationService.class);
        RepositoryFileAggregationService fileAggService = mock(RepositoryFileAggregationService.class);
        RepositoryContributorFileAggregationService contribFileAggService = mock(RepositoryContributorFileAggregationService.class);
        RepositoryFileRiskMaterializationService riskService = mock(RepositoryFileRiskMaterializationService.class);
        RepositoryEvolutionCacheVersionService versionService = mock(RepositoryEvolutionCacheVersionService.class);
        com.gitpulse.config.observability.GitPulseMetrics metrics = new com.gitpulse.config.observability.GitPulseMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry());

        RepositoryAnalysisProcessor processor = new RepositoryAnalysisProcessor(
                jobRepo, commitService, classificationService, fileChangeService,
                contributorService, fileAggService, contribFileAggService, riskService, versionService, metrics
        );

        Long jobId = 100L;
        Long repoId = 50L;
        Repository repo = new Repository("octocat", "hello-world");
        ReflectionTestUtils.setField(repo, "id", repoId);

        AnalysisJob job = new AnalysisJob(repo, AnalysisJobStatus.PENDING);
        ReflectionTestUtils.setField(job, "id", jobId);
        ReflectionTestUtils.setField(job, "createdAt", Instant.now());

        when(jobRepo.findWithRepositoryById(jobId)).thenReturn(Optional.of(job));
        when(commitService.ingestCommits(repoId, jobId)).thenReturn(new CommitIngestionResult(1, 10, 10, 0, 50L));
        when(classificationService.classifyCommits(repoId, jobId)).thenReturn(new CommitClassificationResult(10, 10, 50L));
        when(fileChangeService.ingestFileChanges(repoId, jobId)).thenReturn(new FileChangeIngestionResult(10, 0, 20, 20, 0, 50L));
        when(contributorService.aggregateContributors(repoId, jobId)).thenReturn(new ContributorAggregationResult(2, 2, 2, 0, 50L));
        when(fileAggService.aggregateRepositoryFiles(repoId)).thenReturn(new RepositoryFileAggregationResult(repoId, 5, 5, 0, 0));
        when(contribFileAggService.aggregateRepositoryContributorFiles(repoId)).thenReturn(new RepositoryContributorFileAggregationResult(repoId, 5, 5, 0, 0, 0));
        when(riskService.materializeFileRisks(eq(repoId), any())).thenReturn(new RepositoryFileRiskMaterializationResult(repoId, 5, 5, 0));
        when(versionService.incrementVersion(repoId)).thenReturn(Optional.of(2L));

        processor.processJob(jobId);

        assertThat(job.getStatus()).isEqualTo(AnalysisJobStatus.COMPLETED);
        verify(versionService, times(1)).incrementVersion(repoId);
    }

    @Test
    @DisplayName("Analysis processor should remain COMPLETED when cache version increment fails")
    void analysisProcessor_WhenCacheIncrementFails_JobRemainsCompleted() {
        AnalysisJobJpaRepository jobRepo = mock(AnalysisJobJpaRepository.class);
        CommitIngestionService commitService = mock(CommitIngestionService.class);
        CommitClassificationPipelineService classificationService = mock(CommitClassificationPipelineService.class);
        FileChangeIngestionService fileChangeService = mock(FileChangeIngestionService.class);
        ContributorAggregationService contributorService = mock(ContributorAggregationService.class);
        RepositoryFileAggregationService fileAggService = mock(RepositoryFileAggregationService.class);
        RepositoryContributorFileAggregationService contribFileAggService = mock(RepositoryContributorFileAggregationService.class);
        RepositoryFileRiskMaterializationService riskService = mock(RepositoryFileRiskMaterializationService.class);
        RepositoryEvolutionCacheVersionService versionService = mock(RepositoryEvolutionCacheVersionService.class);
        com.gitpulse.config.observability.GitPulseMetrics metrics = new com.gitpulse.config.observability.GitPulseMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry());

        RepositoryAnalysisProcessor processor = new RepositoryAnalysisProcessor(
                jobRepo, commitService, classificationService, fileChangeService,
                contributorService, fileAggService, contribFileAggService, riskService, versionService, metrics
        );

        Long jobId = 102L;
        Long repoId = 52L;
        Repository repo = new Repository("octocat", "resilient-repo");
        ReflectionTestUtils.setField(repo, "id", repoId);

        AnalysisJob job = new AnalysisJob(repo, AnalysisJobStatus.PENDING);
        ReflectionTestUtils.setField(job, "id", jobId);
        ReflectionTestUtils.setField(job, "createdAt", Instant.now());

        when(jobRepo.findWithRepositoryById(jobId)).thenReturn(Optional.of(job));
        when(commitService.ingestCommits(repoId, jobId)).thenReturn(new CommitIngestionResult(1, 10, 10, 0, 50L));
        when(classificationService.classifyCommits(repoId, jobId)).thenReturn(new CommitClassificationResult(10, 10, 50L));
        when(fileChangeService.ingestFileChanges(repoId, jobId)).thenReturn(new FileChangeIngestionResult(10, 0, 20, 20, 0, 50L));
        when(contributorService.aggregateContributors(repoId, jobId)).thenReturn(new ContributorAggregationResult(2, 2, 2, 0, 50L));
        when(fileAggService.aggregateRepositoryFiles(repoId)).thenReturn(new RepositoryFileAggregationResult(repoId, 5, 5, 0, 0));
        when(contribFileAggService.aggregateRepositoryContributorFiles(repoId)).thenReturn(new RepositoryContributorFileAggregationResult(repoId, 5, 5, 0, 0, 0));
        when(riskService.materializeFileRisks(eq(repoId), any())).thenReturn(new RepositoryFileRiskMaterializationResult(repoId, 5, 5, 0));
        when(versionService.incrementVersion(repoId)).thenReturn(Optional.empty());

        processor.processJob(jobId);

        assertThat(job.getStatus()).isEqualTo(AnalysisJobStatus.COMPLETED);
        assertThat(job.getErrorMessage()).isNull();
        verify(versionService, times(1)).incrementVersion(repoId);
    }

    @Test
    @DisplayName("Analysis processor should not increment cache version on job failure")
    void analysisProcessor_OnFailure_ShouldNotIncrementCacheVersion() {
        AnalysisJobJpaRepository jobRepo = mock(AnalysisJobJpaRepository.class);
        CommitIngestionService commitService = mock(CommitIngestionService.class);
        CommitClassificationPipelineService classificationService = mock(CommitClassificationPipelineService.class);
        FileChangeIngestionService fileChangeService = mock(FileChangeIngestionService.class);
        ContributorAggregationService contributorService = mock(ContributorAggregationService.class);
        RepositoryFileAggregationService fileAggService = mock(RepositoryFileAggregationService.class);
        RepositoryContributorFileAggregationService contribFileAggService = mock(RepositoryContributorFileAggregationService.class);
        RepositoryFileRiskMaterializationService riskService = mock(RepositoryFileRiskMaterializationService.class);
        RepositoryEvolutionCacheVersionService versionService = mock(RepositoryEvolutionCacheVersionService.class);
        com.gitpulse.config.observability.GitPulseMetrics metrics = new com.gitpulse.config.observability.GitPulseMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry());

        RepositoryAnalysisProcessor processor = new RepositoryAnalysisProcessor(
                jobRepo, commitService, classificationService, fileChangeService,
                contributorService, fileAggService, contribFileAggService, riskService, versionService, metrics
        );

        Long jobId = 101L;
        Long repoId = 51L;
        Repository repo = new Repository("octocat", "failed-world");
        ReflectionTestUtils.setField(repo, "id", repoId);

        AnalysisJob job = new AnalysisJob(repo, AnalysisJobStatus.PENDING);
        ReflectionTestUtils.setField(job, "id", jobId);
        ReflectionTestUtils.setField(job, "createdAt", Instant.now());

        when(jobRepo.findWithRepositoryById(jobId)).thenReturn(Optional.of(job));
        when(commitService.ingestCommits(repoId, jobId)).thenThrow(new RuntimeException("GitHub API Error"));

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> processor.processJob(jobId));

        verify(versionService, never()).incrementVersion(anyLong());
    }

    @Test
    @DisplayName("Redis ObjectMapper should successfully serialize and deserialize evolution response DTOs")
    void redisObjectMapper_ShouldRoundTripEvolutionDtos() throws Exception {
        RepositoryEvolutionResponse evolutionResponse = new RepositoryEvolutionResponse(
                1L,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-03-01T00:00:00Z"),
                List.of(new RepositoryMonthlyEvolutionBucketResponse(
                        Instant.parse("2026-01-01T00:00:00Z"),
                        10L, 500L, 200L, 700L, 5L, 2L,
                        1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 1L
                ))
        );

        String json = redisObjectMapper.writeValueAsString(evolutionResponse);
        assertThat(json).isNotBlank();

        Object deserialized = redisObjectMapper.readValue(json, Object.class);
        assertThat(deserialized).isInstanceOf(RepositoryEvolutionResponse.class);
        RepositoryEvolutionResponse typed = (RepositoryEvolutionResponse) deserialized;
        assertThat(typed.repositoryId()).isEqualTo(1L);
        assertThat(typed.buckets()).hasSize(1);
        assertThat(typed.buckets().get(0).month()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    @DisplayName("CacheErrorHandler should safely catch and log cache errors without throwing")
    void cacheErrorHandler_ShouldNotThrowOnErrors() {
        RedisCacheConfig config = new RedisCacheConfig();
        var errorHandler = config.errorHandler();
        Cache cache = mock(Cache.class);
        when(cache.getName()).thenReturn("testCache");

        // All error handlers must execute smoothly without throwing exceptions
        errorHandler.handleCacheGetError(new RuntimeException("Redis GET error"), cache, "testKey");
        errorHandler.handleCachePutError(new RuntimeException("Redis PUT error"), cache, "testKey", "testValue");
        errorHandler.handleCacheEvictError(new RuntimeException("Redis EVICT error"), cache, "testKey");
        errorHandler.handleCacheClearError(new RuntimeException("Redis CLEAR error"), cache);
    }
}
