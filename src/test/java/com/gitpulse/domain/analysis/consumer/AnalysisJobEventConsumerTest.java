package com.gitpulse.domain.analysis.consumer;

import com.gitpulse.domain.analysis.event.AnalysisJobCreatedEvent;
import com.gitpulse.domain.analysis.processor.RepositoryAnalysisProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AnalysisJobEventConsumerTest {

    @Mock
    private RepositoryAnalysisProcessor repositoryAnalysisProcessor;

    @InjectMocks
    private AnalysisJobEventConsumer consumer;

    @Test
    @DisplayName("Should receive AnalysisJobCreatedEvent and delegate job ID to processor")
    void handleAnalysisJobCreated_DelegatesToProcessor() {
        AnalysisJobCreatedEvent event = AnalysisJobCreatedEvent.of(123L, 456L, "octocat/Hello-World");

        consumer.handleAnalysisJobCreated(event, "456", 0, 10L);

        verify(repositoryAnalysisProcessor).processJob(123L);
    }
}
