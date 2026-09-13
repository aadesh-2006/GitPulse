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
    private final String htmlUrl;
    private final String primaryLanguage;
    private final boolean isPrivate;
    private final Instant pushedAt;
    private final Integer starsCount;
    private final Integer forksCount;
    private final Integer openIssuesCount;
    private final Instant createdAt;
    private final Instant updatedAt;

    public RepositoryResponse(Long id, String owner, String name, String fullName,
                              String description, String defaultBranch, Long githubId,
                              String htmlUrl, String primaryLanguage, boolean isPrivate,
                              Instant pushedAt, Integer starsCount, Integer forksCount,
                              Integer openIssuesCount, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.owner = owner;
        this.name = name;
        this.fullName = fullName;
        this.description = description;
        this.defaultBranch = defaultBranch;
        this.githubId = githubId;
        this.htmlUrl = htmlUrl;
        this.primaryLanguage = primaryLanguage;
        this.isPrivate = isPrivate;
        this.pushedAt = pushedAt;
        this.starsCount = starsCount;
        this.forksCount = forksCount;
        this.openIssuesCount = openIssuesCount;
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
                repository.getHtmlUrl(),
                repository.getPrimaryLanguage(),
                repository.isPrivate(),
                repository.getPushedAt(),
                repository.getStarsCount(),
                repository.getForksCount(),
                repository.getOpenIssuesCount(),
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

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public String getPrimaryLanguage() {
        return primaryLanguage;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public Instant getPushedAt() {
        return pushedAt;
    }

    public Integer getStarsCount() {
        return starsCount;
    }

    public Integer getForksCount() {
        return forksCount;
    }

    public Integer getOpenIssuesCount() {
        return openIssuesCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
