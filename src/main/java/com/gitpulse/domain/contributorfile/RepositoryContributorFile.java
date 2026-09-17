package com.gitpulse.domain.contributorfile;

import com.gitpulse.domain.contributor.Contributor;
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
        name = "repository_contributor_files",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_repo_contrib_files",
                        columnNames = {"repository_id", "contributor_id", "file_path"}
                )
        }
)
public class RepositoryContributorFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repository_id", nullable = false)
    private Repository repository;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contributor_id", nullable = false)
    private Contributor contributor;

    @Column(name = "file_path", length = 1000, nullable = false)
    private String filePath;

    @Column(name = "total_revisions", nullable = false)
    private long totalRevisions;

    @Column(name = "total_additions", nullable = false)
    private long totalAdditions;

    @Column(name = "total_deletions", nullable = false)
    private long totalDeletions;

    @Column(name = "total_churn", nullable = false)
    private long totalChurn;

    @Column(name = "first_contributed_at", nullable = false)
    private Instant firstContributedAt;

    @Column(name = "last_contributed_at", nullable = false)
    private Instant lastContributedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RepositoryContributorFile() {
        // Required by JPA
    }

    public RepositoryContributorFile(Repository repository,
                                     Contributor contributor,
                                     String filePath,
                                     long totalRevisions,
                                     long totalAdditions,
                                     long totalDeletions,
                                     long totalChurn,
                                     Instant firstContributedAt,
                                     Instant lastContributedAt) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.contributor = Objects.requireNonNull(contributor, "contributor must not be null");
        this.filePath = Objects.requireNonNull(filePath, "filePath must not be null");
        this.totalRevisions = totalRevisions;
        this.totalAdditions = totalAdditions;
        this.totalDeletions = totalDeletions;
        this.totalChurn = totalChurn;
        this.firstContributedAt = Objects.requireNonNull(firstContributedAt, "firstContributedAt must not be null");
        this.lastContributedAt = Objects.requireNonNull(lastContributedAt, "lastContributedAt must not be null");
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

    public boolean hasMetricsChanged(long totalRevisions,
                                     long totalAdditions,
                                     long totalDeletions,
                                     long totalChurn,
                                     Instant firstContributedAt,
                                     Instant lastContributedAt) {
        return this.totalRevisions != totalRevisions
                || this.totalAdditions != totalAdditions
                || this.totalDeletions != totalDeletions
                || this.totalChurn != totalChurn
                || !Objects.equals(this.firstContributedAt, firstContributedAt)
                || !Objects.equals(this.lastContributedAt, lastContributedAt);
    }

    public void updateMetrics(long totalRevisions,
                              long totalAdditions,
                              long totalDeletions,
                              long totalChurn,
                              Instant firstContributedAt,
                              Instant lastContributedAt) {
        this.totalRevisions = totalRevisions;
        this.totalAdditions = totalAdditions;
        this.totalDeletions = totalDeletions;
        this.totalChurn = totalChurn;
        if (firstContributedAt != null) {
            this.firstContributedAt = firstContributedAt;
        }
        if (lastContributedAt != null) {
            this.lastContributedAt = lastContributedAt;
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

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getTotalRevisions() {
        return totalRevisions;
    }

    public void setTotalRevisions(long totalRevisions) {
        this.totalRevisions = totalRevisions;
    }

    public long getTotalAdditions() {
        return totalAdditions;
    }

    public void setTotalAdditions(long totalAdditions) {
        this.totalAdditions = totalAdditions;
    }

    public long getTotalDeletions() {
        return totalDeletions;
    }

    public void setTotalDeletions(long totalDeletions) {
        this.totalDeletions = totalDeletions;
    }

    public long getTotalChurn() {
        return totalChurn;
    }

    public void setTotalChurn(long totalChurn) {
        this.totalChurn = totalChurn;
    }

    public Instant getFirstContributedAt() {
        return firstContributedAt;
    }

    public void setFirstContributedAt(Instant firstContributedAt) {
        this.firstContributedAt = firstContributedAt;
    }

    public Instant getLastContributedAt() {
        return lastContributedAt;
    }

    public void setLastContributedAt(Instant lastContributedAt) {
        this.lastContributedAt = lastContributedAt;
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
        if (!(o instanceof RepositoryContributorFile that)) return false;
        return Objects.equals(repository != null ? repository.getId() : null, that.repository != null ? that.repository.getId() : null)
                && Objects.equals(contributor != null ? contributor.getId() : null, that.contributor != null ? that.contributor.getId() : null)
                && Objects.equals(filePath, that.filePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                repository != null ? repository.getId() : null,
                contributor != null ? contributor.getId() : null,
                filePath
        );
    }

    @Override
    public String toString() {
        return "RepositoryContributorFile{" +
                "id=" + id +
                ", repositoryId=" + (repository != null ? repository.getId() : null) +
                ", contributorId=" + (contributor != null ? contributor.getId() : null) +
                ", filePath='" + filePath + '\'' +
                ", totalRevisions=" + totalRevisions +
                ", totalChurn=" + totalChurn +
                '}';
    }
}
