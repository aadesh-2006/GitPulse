package com.gitpulse.domain.analysis.consumer;

import com.gitpulse.domain.analysis.event.AnalysisJobCreatedEvent;
import com.gitpulse.domain.analysis.processor.RepositoryAnalysisProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class AnalysisJobEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AnalysisJobEventConsumer.class);

    private final RepositoryAnalysisProcessor repositoryAnalysisProcessor;

    public AnalysisJobEventConsumer(RepositoryAnalysisProcessor repositoryAnalysisProcessor) {
        this.repositoryAnalysisProcessor = repositoryAnalysisProcessor;
    }

    @KafkaListener(
            topics = "${kafka.topics.analysis-jobs:analysis-jobs}",
            groupId = "${kafka.consumer.group-id:gitpulse-analysis-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleAnalysisJobCreated(
            @Payload AnalysisJobCreatedEvent event,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key,
            @Header(value = KafkaHeaders.RECEIVED_PARTITION, required = false) Integer partition,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset) {

        log.info("Received AnalysisJobCreatedEvent [eventId={}, jobId={}, repoId={}] from partition={}, offset={}",
                event.getEventId(), event.getAnalysisJobId(), event.getRepositoryId(), partition, offset);

        repositoryAnalysisProcessor.processJob(event.getAnalysisJobId());
    }
}
