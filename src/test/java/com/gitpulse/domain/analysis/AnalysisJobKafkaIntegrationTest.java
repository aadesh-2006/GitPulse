package com.gitpulse.domain.analysis;

import com.gitpulse.domain.analysis.dto.AnalysisJobResponse;
import com.gitpulse.domain.repository.Repository;
import com.gitpulse.domain.repository.RepositoryJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.listener.auto-startup=true",
        "kafka.topics.auto-create=true"
})
@EmbeddedKafka(partitions = 1)
@ActiveProfiles("test")
@DirtiesContext
class AnalysisJobKafkaIntegrationTest {

    @Autowired
    private AnalysisJobService analysisJobService;

    @Autowired
    private AnalysisJobJpaRepository analysisJobJpaRepository;

    @Autowired
    private RepositoryJpaRepository repositoryJpaRepository;

    @Test
    @DisplayName("End-to-End: AnalysisJob creation should publish Kafka event and consumer should transition job to COMPLETED")
    void endToEndAnalysisJobProcessing() {
        Repository repo = repositoryJpaRepository.save(new Repository("apache", "flink", "Stateful computations over data streams", "master"));

        AnalysisJobResponse createdJob = analysisJobService.createAnalysisJob(repo.getId());

        assertThat(createdJob).isNotNull();
        assertThat(createdJob.getStatus()).isEqualTo(AnalysisJobStatus.PENDING);
        assertThat(createdJob.getRepositoryId()).isEqualTo(repo.getId());

        // Await asynchronous processing by Kafka consumer
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
                });
    }
}
