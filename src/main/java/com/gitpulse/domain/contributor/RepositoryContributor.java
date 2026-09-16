package com.gitpulse.domain.contributor;

import com.gitpulse.domain.repository.Repository;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "repository_contributors",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_repo_contrib",
                        columnNames = {"repository_id", "contributor_id"}
                )
        }
)
public class RepositoryContributor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repository_id", nullable = false)
    private Repository repository;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contributor_id", nullable = false)
    private Contributor contributor;

    @Column(name = "total_commits", nullable = false)
    private int totalCommits;

    @Column(name = "total_additions", nullable = false)
    private int totalAdditions;

    @Column(name = "total_deletions", nullable = false)
    private int totalDeletions;

    @Column(name = "total_changes", nullable = false)
    private int totalChanges;

    @Column(name = "first_committed_at", nullable = false)
    private Instant firstCommittedAt;

    @Column(name = "last_committed_at", nullable = false)
    private Instant lastCommittedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RepositoryContributor() {
        // Required by JPA
    }

    public RepositoryContributor(Repository repository,
                                 Contributor contributor,
                                 int totalCommits,
                                 int totalAdditions,
                                 int totalDeletions,
                                 int totalChanges,
                                 Instant firstCommittedAt,
                                 Instant lastCommittedAt) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.contributor = Objects.requireNonNull(contributor, "contributor must not be null");
        this.totalCommits = totalCommits;
        this.totalAdditions = totalAdditions;
        this.totalDeletions = totalDeletions;
        this.totalChanges = totalChanges;
        this.firstCommittedAt = Objects.requireNonNull(firstCommittedAt, "firstCommittedAt must not be null");
        this.lastCommittedAt = Objects.requireNonNull(lastCommittedAt, "lastCommittedAt must not be null");
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void updateMetrics(int totalCommits,
                              int totalAdditions,
                              int totalDeletions,
                              int totalChanges,
                              Instant firstCommittedAt,
                              Instant lastCommittedAt) {
        this.totalCommits = totalCommits;
        this.totalAdditions = totalAdditions;
        this.totalDeletions = totalDeletions;
        this.totalChanges = totalChanges;
        if (firstCommittedAt != null) {
            this.firstCommittedAt = firstCommittedAt;
        }
        if (lastCommittedAt != null) {
            this.lastCommittedAt = lastCommittedAt;
        }
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public Contributor getContributor() {
        return contributor;
    }

    public void setContributor(Contributor contributor) {
        this.contributor = contributor;
    }

    public int getTotalCommits() {
        return totalCommits;
    }

    public int getTotalAdditions() {
        return totalAdditions;
    }

    public int getTotalDeletions() {
        return totalDeletions;
    }

    public int getTotalChanges() {
        return totalChanges;
    }

    public Instant getFirstCommittedAt() {
        return firstCommittedAt;
    }

    public Instant getLastCommittedAt() {
        return lastCommittedAt;
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
        if (!(o instanceof RepositoryContributor that)) return false;
        return Objects.equals(repository != null ? repository.getId() : null, that.repository != null ? that.repository.getId() : null)
                && Objects.equals(contributor != null ? contributor.getId() : null, that.contributor != null ? that.contributor.getId() : null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(repository != null ? repository.getId() : null, contributor != null ? contributor.getId() : null);
    }

    @Override
    public String toString() {
        return "RepositoryContributor{" +
                "id=" + id +
                ", repositoryId=" + (repository != null ? repository.getId() : null) +
                ", contributorId=" + (contributor != null ? contributor.getId() : null) +
                ", totalCommits=" + totalCommits +
                '}';
    }
}
