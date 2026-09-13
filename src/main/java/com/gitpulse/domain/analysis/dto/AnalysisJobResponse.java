package com.gitpulse.domain.analysis.dto;

import com.gitpulse.domain.analysis.AnalysisJob;
import com.gitpulse.domain.analysis.AnalysisJobStatus;

import java.time.Instant;

public class AnalysisJobResponse {

    private final Long id;
    private final Long repositoryId;
    private final String repositoryFullName;
    private final AnalysisJobStatus status;
    private final Instant startedAt;
    private final Instant completedAt;
    private final String errorMessage;
    private final Instant createdAt;
    private final Instant updatedAt;

    public AnalysisJobResponse(Long id, Long repositoryId, String repositoryFullName,
                               AnalysisJobStatus status, Instant startedAt, Instant completedAt,
                               String errorMessage, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.repositoryId = repositoryId;
        this.repositoryFullName = repositoryFullName;
        this.status = status;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static AnalysisJobResponse fromEntity(AnalysisJob job) {
        if (job == null) {
            return null;
        }
        return new AnalysisJobResponse(
                job.getId(),
                job.getRepository() != null ? job.getRepository().getId() : null,
                job.getRepository() != null ? job.getRepository().getFullName() : null,
                job.getStatus(),
                job.getStartedAt(),
                job.getCompletedAt(),
                job.getErrorMessage(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public Long getRepositoryId() {
        return repositoryId;
    }

    public String getRepositoryFullName() {
        return repositoryFullName;
    }

    public AnalysisJobStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
