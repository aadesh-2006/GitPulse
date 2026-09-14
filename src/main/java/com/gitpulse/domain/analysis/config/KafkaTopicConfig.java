package com.gitpulse.domain.analysis.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(name = "kafka.topics.auto-create", havingValue = "true", matchIfMissing = true)
public class KafkaTopicConfig {

    @Value("${kafka.topics.analysis-jobs:analysis-jobs}")
    private String analysisJobsTopic;

    @Value("${kafka.topics.analysis-jobs-dlt:analysis-jobs.DLT}")
    private String analysisJobsDltTopic;

    @Bean
    public NewTopic analysisJobsTopic() {
        return TopicBuilder.name(analysisJobsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic analysisJobsDltTopic() {
        return TopicBuilder.name(analysisJobsDltTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
