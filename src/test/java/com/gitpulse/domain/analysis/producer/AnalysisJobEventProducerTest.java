package com.gitpulse.domain.analysis.producer;

import com.gitpulse.domain.analysis.event.AnalysisJobCreatedEvent;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisJobEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private AnalysisJobEventProducer producer;
    private static final String TOPIC = "analysis-jobs";

    @BeforeEach
    void setUp() {
        producer = new AnalysisJobEventProducer(kafkaTemplate, TOPIC);
    }

    @Test
    @DisplayName("Should publish AnalysisJobCreatedEvent to Kafka topic with repository ID as partition key")
    void sendAnalysisJobCreatedEvent_Success() {
        AnalysisJobCreatedEvent event = AnalysisJobCreatedEvent.of(100L, 5L, "spring-projects/spring-boot");

        RecordMetadata metadata = new RecordMetadata(new TopicPartition(TOPIC, 0), 0, 0, 0, 0, 0);
        SendResult<String, Object> sendResult = new SendResult<>(null, metadata);
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);

        when(kafkaTemplate.send(eq(TOPIC), eq("5"), eq(event))).thenReturn(future);

        producer.sendAnalysisJobCreatedEvent(event);

        ArgumentCaptor<AnalysisJobCreatedEvent> eventCaptor = ArgumentCaptor.forClass(AnalysisJobCreatedEvent.class);
        verify(kafkaTemplate).send(eq(TOPIC), eq("5"), eventCaptor.capture());

        AnalysisJobCreatedEvent captured = eventCaptor.getValue();
        assertThat(captured.getAnalysisJobId()).isEqualTo(100L);
        assertThat(captured.getRepositoryId()).isEqualTo(5L);
        assertThat(captured.getRepositoryFullName()).isEqualTo("spring-projects/spring-boot");
        assertThat(captured.getEventVersion()).isEqualTo("1.0");
    }
}
