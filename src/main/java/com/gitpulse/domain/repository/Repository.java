package com.gitpulse.domain.repository;

import com.gitpulse.integration.github.dto.GitHubRepositoryResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "repositories")
public class Repository {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner", nullable = false, length = 100)
    private String owner;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "full_name", nullable = false, unique = true, length = 200)
    private String fullName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "default_branch", nullable = false, length = 100)
    private String defaultBranch = "main";

    @Column(name = "github_id")
    private Long githubId;

    @Column(name = "html_url", length = 300)
    private String htmlUrl;

    @Column(name = "primary_language", length = 100)
    private String primaryLanguage;

    @Column(name = "is_private", nullable = false)
    private boolean isPrivate = false;

    @Column(name = "pushed_at")
    private Instant pushedAt;

    @Column(name = "stars_count")
    private Integer starsCount = 0;

    @Column(name = "forks_count")
    private Integer forksCount = 0;

    @Column(name = "open_issues_count")
    private Integer openIssuesCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Repository() {
        // Required by JPA
    }

    public Repository(String owner, String name) {
        this(owner, name, null, "main");
    }

    public Repository(String owner, String name, String description, String defaultBranch) {
        this.owner = Objects.requireNonNull(owner, "owner must not be null").trim();
        this.name = Objects.requireNonNull(name, "name must not be null").trim();
        this.fullName = this.owner + "/" + this.name;
        this.description = description != null ? description.trim() : null;
        this.defaultBranch = (defaultBranch != null && !defaultBranch.isBlank()) ? defaultBranch.trim() : "main";
    }

    public void updateFromGitHub(GitHubRepositoryResponse githubData) {
        if (githubData == null) {
            return;
        }
        if (githubData.getId() != null) {
            this.githubId = githubData.getId();
        }
        if (githubData.getDescription() != null) {
            this.description = githubData.getDescription();
        }
        if (githubData.getDefaultBranch() != null && !githubData.getDefaultBranch().isBlank()) {
            this.defaultBranch = githubData.getDefaultBranch();
        }
        if (githubData.getHtmlUrl() != null) {
            this.htmlUrl = githubData.getHtmlUrl();
        }
        if (githubData.getLanguage() != null) {
            this.primaryLanguage = githubData.getLanguage();
        }
        if (githubData.getIsPrivate() != null) {
            this.isPrivate = githubData.getIsPrivate();
        }
        if (githubData.getPushedAt() != null) {
            this.pushedAt = githubData.getPushedAt();
        }
        if (githubData.getStargazersCount() != null) {
            this.starsCount = githubData.getStargazersCount();
        }
        if (githubData.getForksCount() != null) {
            this.forksCount = githubData.getForksCount();
        }
        if (githubData.getOpenIssuesCount() != null) {
            this.openIssuesCount = githubData.getOpenIssuesCount();
        }
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.fullName == null && this.owner != null && this.name != null) {
            this.fullName = this.owner + "/" + this.name;
        }
        if (this.defaultBranch == null || this.defaultBranch.isBlank()) {
            this.defaultBranch = "main";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    public Long getGithubId() {
        return githubId;
    }

    public void setGithubId(Long githubId) {
        this.githubId = githubId;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public String getPrimaryLanguage() {
        return primaryLanguage;
    }

    public void setPrimaryLanguage(String primaryLanguage) {
        this.primaryLanguage = primaryLanguage;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }

    public Instant getPushedAt() {
        return pushedAt;
    }

    public void setPushedAt(Instant pushedAt) {
        this.pushedAt = pushedAt;
    }

    public Integer getStarsCount() {
        return starsCount;
    }

    public void setStarsCount(Integer starsCount) {
        this.starsCount = starsCount;
    }

    public Integer getForksCount() {
        return forksCount;
    }

    public void setForksCount(Integer forksCount) {
        this.forksCount = forksCount;
    }

    public Integer getOpenIssuesCount() {
        return openIssuesCount;
    }

    public void setOpenIssuesCount(Integer openIssuesCount) {
        this.openIssuesCount = openIssuesCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Repository that)) return false;
        return Objects.equals(fullName, that.fullName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullName);
    }
}
