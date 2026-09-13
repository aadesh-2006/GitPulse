package com.gitpulse.domain.repository.dto;

import com.gitpulse.domain.repository.Repository;

import java.time.Instant;

public class RepositoryResponse {

    private final Long id;
    private final String owner;
    private final String name;
    private final String fullName;
    private final String description;
    private final String defaultBranch;
    private final Long githubId;
    private final Instant createdAt;
    private final Instant updatedAt;

    public RepositoryResponse(Long id, String owner, String name, String fullName,
                              String description, String defaultBranch, Long githubId,
                              Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.owner = owner;
        this.name = name;
        this.fullName = fullName;
        this.description = description;
        this.defaultBranch = defaultBranch;
        this.githubId = githubId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static RepositoryResponse fromEntity(Repository repository) {
        if (repository == null) {
            return null;
        }
        return new RepositoryResponse(
                repository.getId(),
                repository.getOwner(),
                repository.getName(),
                repository.getFullName(),
                repository.getDescription(),
                repository.getDefaultBranch(),
                repository.getGithubId(),
                repository.getCreatedAt(),
                repository.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public String getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return fullName;
    }

    public String getDescription() {
        return description;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public Long getGithubId() {
        return githubId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
