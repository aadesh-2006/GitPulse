package com.gitpulse.domain.analysis;

import com.gitpulse.common.exception.ResourceNotFoundException;
import com.gitpulse.domain.analysis.dto.AnalysisJobResponse;
import com.gitpulse.domain.analysis.event.AnalysisJobCreatedEvent;
import com.gitpulse.domain.analysis.producer.AnalysisJobEventProducer;
import com.gitpulse.domain.repository.Repository;
import com.gitpulse.domain.repository.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AnalysisJobService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisJobService.class);

    private final AnalysisJobJpaRepository analysisJobJpaRepository;
    private final RepositoryService repositoryService;
    private final AnalysisJobEventProducer analysisJobEventProducer;

    public AnalysisJobService(
            AnalysisJobJpaRepository analysisJobJpaRepository,
            RepositoryService repositoryService,
            AnalysisJobEventProducer analysisJobEventProducer) {
        this.analysisJobJpaRepository = analysisJobJpaRepository;
        this.repositoryService = repositoryService;
        this.analysisJobEventProducer = analysisJobEventProducer;
    }

    @Transactional
    public AnalysisJobResponse createAnalysisJob(Long repositoryId) {
        Repository repository = repositoryService.findEntityById(repositoryId);

        AnalysisJob job = new AnalysisJob(repository, AnalysisJobStatus.PENDING);
        AnalysisJob saved = analysisJobJpaRepository.save(job);

        log.info("Created PENDING analysis job [id={}, repositoryId={}]", saved.getId(), repository.getId());

        // Publish event to Kafka for asynchronous pipeline execution
        AnalysisJobCreatedEvent event = AnalysisJobCreatedEvent.of(
                saved.getId(),
                repository.getId(),
                repository.getFullName()
        );
        analysisJobEventProducer.sendAnalysisJobCreatedEvent(event);

        return AnalysisJobResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public AnalysisJobResponse getAnalysisJobById(Long jobId) {
        AnalysisJob job = analysisJobJpaRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("AnalysisJob", "id", jobId));

        return AnalysisJobResponse.fromEntity(job);
    }

    @Transactional(readOnly = true)
    public List<AnalysisJobResponse> getJobsByRepositoryId(Long repositoryId) {
        // Ensure repository exists
        repositoryService.findEntityById(repositoryId);

        return analysisJobJpaRepository.findByRepositoryIdOrderByCreatedAtDesc(repositoryId)
                .stream()
                .map(AnalysisJobResponse::fromEntity)
                .toList();
    }
}
