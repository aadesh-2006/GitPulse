package com.gitpulse.domain.analysis.producer;

import com.gitpulse.domain.analysis.event.AnalysisJobCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class AnalysisJobEventProducer {

    private static final Logger log = LoggerFactory.getLogger(AnalysisJobEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String analysisJobsTopic;

    public AnalysisJobEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${kafka.topics.analysis-jobs:analysis-jobs}") String analysisJobsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.analysisJobsTopic = analysisJobsTopic;
    }

    public void sendAnalysisJobCreatedEvent(AnalysisJobCreatedEvent event) {
        Objects.requireNonNull(event, "event must not be null");

        String partitionKey = String.valueOf(event.getRepositoryId());

        log.info("Publishing AnalysisJobCreatedEvent [eventId={}, jobId={}, repositoryId={}] to topic '{}'",
                event.getEventId(), event.getAnalysisJobId(), event.getRepositoryId(), analysisJobsTopic);

        kafkaTemplate.send(analysisJobsTopic, partitionKey, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Successfully published AnalysisJobCreatedEvent [jobId={}] to topic '{}' [partition={}, offset={}]",
                                event.getAnalysisJobId(),
                                analysisJobsTopic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to publish AnalysisJobCreatedEvent [jobId={}] to topic '{}': {}",
                                event.getAnalysisJobId(), analysisJobsTopic, ex.getMessage(), ex);
                    }
                });
    }
}
