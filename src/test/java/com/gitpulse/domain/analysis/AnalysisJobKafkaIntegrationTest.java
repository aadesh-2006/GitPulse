package com.gitpulse.domain.analysis;

import com.gitpulse.domain.analysis.dto.AnalysisJobResponse;
import com.gitpulse.domain.commit.Commit;
import com.gitpulse.domain.commit.CommitClassification;
import com.gitpulse.domain.commit.CommitJpaRepository;
import com.gitpulse.domain.contributor.ContributorJpaRepository;
import com.gitpulse.domain.contributor.RepositoryContributorJpaRepository;
import com.gitpulse.domain.contributorfile.RepositoryContributorFileJpaRepository;
import com.gitpulse.domain.evolution.cache.RepositoryEvolutionCacheVersionService;
import com.gitpulse.domain.file.RepositoryFileJpaRepository;
import com.gitpulse.domain.filechange.FileChangeJpaRepository;
import com.gitpulse.domain.repository.Repository;
import com.gitpulse.domain.repository.RepositoryJpaRepository;
import com.gitpulse.integration.github.client.GitHubCommitClient;
import com.gitpulse.integration.github.client.GitHubCommitDetailsClient;
import com.gitpulse.integration.github.dto.GitHubCommitDetailResponse;
import com.gitpulse.integration.github.dto.GitHubCommitPageResponse;
import com.gitpulse.integration.github.dto.GitHubCommitResponse;
import com.gitpulse.integration.github.dto.GitHubFileResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=true",
        "kafka.topics.auto-create=true"
})
@EmbeddedKafka(partitions = 1, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@ActiveProfiles("test")
@DirtiesContext
class AnalysisJobKafkaIntegrationTest {

    @Autowired
    private AnalysisJobService analysisJobService;

    @Autowired
    private AnalysisJobJpaRepository analysisJobJpaRepository;

    @Autowired
    private RepositoryJpaRepository repositoryJpaRepository;

    @Autowired
    private CommitJpaRepository commitJpaRepository;

    @Autowired
    private FileChangeJpaRepository fileChangeJpaRepository;

    @Autowired
    private ContributorJpaRepository contributorJpaRepository;

    @Autowired
    private RepositoryContributorJpaRepository repositoryContributorJpaRepository;

    @Autowired
    private RepositoryContributorFileJpaRepository repositoryContributorFileJpaRepository;

    @Autowired
    private RepositoryFileJpaRepository repositoryFileJpaRepository;

    @MockBean
    private GitHubCommitClient gitHubCommitClient;

    @MockBean
    private GitHubCommitDetailsClient gitHubCommitDetailsClient;

    @MockBean
    private RepositoryEvolutionCacheVersionService cacheVersionService;

    @Test
    @DisplayName("End-to-End: AnalysisJob creation should publish Kafka event, ingest commits, ingest file changes, aggregate contributors, aggregate files, and transition job to COMPLETED")
    void endToEndAnalysisJobProcessing() {
        Repository repo = repositoryJpaRepository.save(new Repository("kafka-test-e2e-org", "flink-e2e", "Stateful computations over data streams", "master"));

        GitHubCommitResponse.GitUser gitUser = new GitHubCommitResponse.GitUser("Flink Dev", "dev@flink.apache.org", Instant.now());
        GitHubCommitResponse.CommitDetails details = new GitHubCommitResponse.CommitDetails("FLINK-1234: Add streaming feature", gitUser, gitUser);
        GitHubCommitResponse.GitHubUser ghUser = new GitHubCommitResponse.GitHubUser("flinkdev", 99L);
        String sha = "e2e_commit_sha_123456789012345678901234";

        GitHubCommitResponse commitResponse = new GitHubCommitResponse(
                sha,
                "https://github.com/apache/flink/commit/" + sha,
                details,
                ghUser,
                ghUser,
                null
        );

        when(gitHubCommitClient.getCommitsPage(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(new GitHubCommitPageResponse(List.of(commitResponse), false));

        GitHubFileResponse fileResponse = new GitHubFileResponse(
                "flink-core/src/main/java/FlinkApp.java",
                "added",
                25,
                0,
                25,
                "https://github.com/apache/flink/blob/" + sha + "/flink-core/src/main/java/FlinkApp.java",
                "https://github.com/apache/flink/raw/" + sha + "/flink-core/src/main/java/FlinkApp.java",
                null
        );
        GitHubCommitDetailResponse detailResponse = new GitHubCommitDetailResponse(
                sha,
                "https://github.com/apache/flink/commit/" + sha,
                details,
                ghUser,
                ghUser,
                new GitHubCommitResponse.CommitStats(25, 0, 25),
                List.of(fileResponse)
        );

        when(gitHubCommitDetailsClient.getCommitDetails(anyString(), anyString(), anyString()))
                .thenReturn(detailResponse);

        AnalysisJobResponse createdJob = analysisJobService.createAnalysisJob(repo.getId());

        assertThat(createdJob).isNotNull();
        assertThat(createdJob.getStatus()).isEqualTo(AnalysisJobStatus.PENDING);
        assertThat(createdJob.getRepositoryId()).isEqualTo(repo.getId());

        // Await asynchronous processing by Kafka consumer: commit ingestion -> file-change ingestion -> contributor aggregation -> file aggregation
        await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    AnalysisJob job = analysisJobJpaRepository.findById(createdJob.getId()).orElse(null);
                    assertThat(job).isNotNull();
                    assertThat(job.getStatus()).isEqualTo(AnalysisJobStatus.COMPLETED);
                    assertThat(job.getStartedAt()).isNotNull();
                    assertThat(job.getCompletedAt()).isNotNull();
                    assertThat(job.getErrorMessage()).isNull();

                    // 1. Commits verified
                    assertThat(commitJpaRepository.countByRepositoryId(repo.getId())).isEqualTo(1);
                    assertThat(commitJpaRepository.existsByRepositoryIdAndGithubCommitSha(repo.getId(), sha)).isTrue();
                    Commit savedCommit = commitJpaRepository.findByRepositoryIdOrderByCommittedAtDesc(repo.getId(), org.springframework.data.domain.PageRequest.of(0, 1)).getContent().get(0);
                    assertThat(savedCommit.getClassification()).isEqualTo(CommitClassification.FEATURE);

                    // 2. File changes verified
                    assertThat(fileChangeJpaRepository.count()).isEqualTo(1);

                    // 3. Contributor attribution verified
                    assertThat(contributorJpaRepository.count()).isEqualTo(1);
                    assertThat(repositoryContributorJpaRepository.countByRepositoryId(repo.getId())).isEqualTo(1);
                    var rc = repositoryContributorJpaRepository.findByRepositoryId(repo.getId()).get(0);
                    assertThat(rc.getContributor().getEmail()).isEqualTo("dev@flink.apache.org");
                    assertThat(rc.getTotalCommits()).isEqualTo(1);
                    assertThat(rc.getTotalChanges()).isEqualTo(25);

                    // 4. Materialized repository file verified
                    var repoFiles = repositoryFileJpaRepository.findByRepositoryId(repo.getId());
                    assertThat(repoFiles).hasSize(1);
                    var rf = repoFiles.get(0);
                    assertThat(rf.getFilePath()).isEqualTo("flink-core/src/main/java/FlinkApp.java");
                    assertThat(rf.getFileName()).isEqualTo("FlinkApp.java");
                    assertThat(rf.getExtension()).isEqualTo("java");
                    assertThat(rf.getDirectoryPath()).isEqualTo("flink-core/src/main/java");
                    assertThat(rf.getTotalRevisions()).isEqualTo(1);
                    assertThat(rf.getTotalAdditions()).isEqualTo(25);
                    assertThat(rf.getTotalDeletions()).isEqualTo(0);
                    assertThat(rf.getTotalChurn()).isEqualTo(25);
                    assertThat(rf.isDeleted()).isFalse();
                    assertThat(rf.getPrimaryContributor()).isNotNull();
                    assertThat(rf.getPrimaryContributor().getEmail()).isEqualTo("dev@flink.apache.org");

                    // 5. Materialized contributor-file attribution verified
                    assertThat(repositoryContributorFileJpaRepository.countByRepositoryId(repo.getId())).isEqualTo(1);
                    var contributorFiles = repositoryContributorFileJpaRepository.findByRepositoryId(repo.getId());
                    assertThat(contributorFiles).hasSize(1);
                    var rcf = contributorFiles.get(0);
                    assertThat(rcf.getFilePath()).isEqualTo("flink-core/src/main/java/FlinkApp.java");
                    assertThat(rcf.getContributor().getEmail()).isEqualTo("dev@flink.apache.org");
                    assertThat(rcf.getTotalRevisions()).isEqualTo(1);
                    assertThat(rcf.getTotalChurn()).isEqualTo(25);

                    // 6. Deterministic file risk scores verified
                    assertThat(rf.getCompositeScore()).isGreaterThan(0.0);
                    assertThat(rf.getBaselineScore()).isGreaterThan(0.0);
                    assertThat(rf.getRevisionFrequencyScore()).isGreaterThan(0.0);
                    assertThat(rf.getChurnScore()).isGreaterThan(0.0);
                    assertThat(rf.getRecencyScore()).isGreaterThan(0.0);
                    assertThat(rf.getOwnershipConcentrationScore()).isGreaterThan(0.0);
                });
    }

    @Test
    @DisplayName("Kafka Retry: Transient stage failure should transition job to FAILED, retry via Kafka, and transition to COMPLETED")
    void transientFailure_ShouldRetryAndEventuallyComplete() {
        Repository repo = repositoryJpaRepository.save(new Repository("kafka-test-retry-org", "spark-retry", "Unified engine for large-scale data analytics", "master"));

        GitHubCommitResponse.GitUser gitUser = new GitHubCommitResponse.GitUser("Spark Dev", "dev@spark.apache.org", Instant.now());
        GitHubCommitResponse.CommitDetails details = new GitHubCommitResponse.CommitDetails("SPARK-100: Initial commit", gitUser, gitUser);
        GitHubCommitResponse.GitHubUser ghUser = new GitHubCommitResponse.GitHubUser("sparkdev", 101L);
        String sha = "e2e_retry_commit_sha_1234567890123456";

        GitHubCommitResponse commitResponse = new GitHubCommitResponse(
                sha, "https://github.com/apache/spark/commit/" + sha, details, ghUser, ghUser, null
        );
        GitHubFileResponse fileResponse = new GitHubFileResponse(
                "core/src/main/scala/SparkContext.scala", "added", 50, 0, 50,
                "https://github.com/apache/spark/blob/" + sha + "/core/src/main/scala/SparkContext.scala",
                "https://github.com/apache/spark/raw/" + sha + "/core/src/main/scala/SparkContext.scala", null
        );
        GitHubCommitDetailResponse detailResponse = new GitHubCommitDetailResponse(
                sha, "https://github.com/apache/spark/commit/" + sha, details, ghUser, ghUser,
                new GitHubCommitResponse.CommitStats(50, 0, 50), List.of(fileResponse)
        );

        AtomicInteger attemptCounter = new AtomicInteger(0);
        when(gitHubCommitClient.getCommitsPage(anyString(), anyString(), anyInt(), anyInt()))
                .thenAnswer(invocation -> {
                    int attempt = attemptCounter.incrementAndGet();
                    if (attempt == 1) {
                        throw new RuntimeException("Transient 503 GitHub API Error");
                    }
                    return new GitHubCommitPageResponse(List.of(commitResponse), false);
                });

        when(gitHubCommitDetailsClient.getCommitDetails(anyString(), anyString(), anyString()))
                .thenReturn(detailResponse);

        AnalysisJobResponse createdJob = analysisJobService.createAnalysisJob(repo.getId());

        // Await Kafka retry delivery and successful transition to COMPLETED
        await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(300))
                .untilAsserted(() -> {
                    AnalysisJob job = analysisJobJpaRepository.findById(createdJob.getId()).orElse(null);
                    assertThat(job).isNotNull();
                    assertThat(job.getStatus()).isEqualTo(AnalysisJobStatus.COMPLETED);
                    assertThat(job.getErrorMessage()).isNull();
                    assertThat(attemptCounter.get()).isGreaterThanOrEqualTo(2);
                });
    }

    @Test
    @DisplayName("Kafka Retry: Persistent failure should exhaust retries, route to DLT, leave job FAILED, and not increment cache version")
    void persistentFailure_ShouldExhaustRetriesAndRemainFailed() {
        Repository repo = repositoryJpaRepository.save(new Repository("kafka-test-dlt-org", "kafka-dlt", "Distributed event streaming platform", "trunk"));

        when(gitHubCommitClient.getCommitsPage(anyString(), anyString(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("Permanent 500 GitHub Service Failure"));

        AnalysisJobResponse createdJob = analysisJobService.createAnalysisJob(repo.getId());

        await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(300))
                .untilAsserted(() -> {
                    AnalysisJob job = analysisJobJpaRepository.findById(createdJob.getId()).orElse(null);
                    assertThat(job).isNotNull();
                    assertThat(job.getStatus()).isEqualTo(AnalysisJobStatus.FAILED);
                    assertThat(job.getErrorMessage()).contains("Permanent 500 GitHub Service Failure");
                    verify(cacheVersionService, never()).incrementVersion(anyLong());
                });
    }
}