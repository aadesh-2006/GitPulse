package com.gitpulse.domain.analysis;

import com.gitpulse.domain.analysis.dto.AnalysisJobResponse;
import com.gitpulse.domain.commit.CommitJpaRepository;
import com.gitpulse.domain.repository.Repository;
import com.gitpulse.domain.repository.RepositoryJpaRepository;
import com.gitpulse.integration.github.client.GitHubCommitClient;
import com.gitpulse.integration.github.dto.GitHubCommitPageResponse;
import com.gitpulse.integration.github.dto.GitHubCommitResponse;
import com.gitpulse.domain.filechange.FileChangeJpaRepository;
import com.gitpulse.integration.github.client.GitHubCommitDetailsClient;
import com.gitpulse.integration.github.dto.GitHubCommitDetailResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
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

    @MockBean
    private GitHubCommitClient gitHubCommitClient;

    @MockBean
    private GitHubCommitDetailsClient gitHubCommitDetailsClient;

    @Test
    @DisplayName("End-to-End: AnalysisJob creation should publish Kafka event, ingest commits, ingest file changes, and transition job to COMPLETED")
    void endToEndAnalysisJobProcessing() {
        Repository repo = repositoryJpaRepository.save(new Repository("apache", "flink", "Stateful computations over data streams", "master"));

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

        // Await asynchronous processing by Kafka consumer, commit ingestion, and file-change ingestion
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

                    assertThat(commitJpaRepository.countByRepositoryId(repo.getId())).isEqualTo(1);
                    assertThat(commitJpaRepository.existsByRepositoryIdAndGithubCommitSha(repo.getId(), sha)).isTrue();

                    assertThat(fileChangeJpaRepository.count()).isEqualTo(1);
                });
    }
}
