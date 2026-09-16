package com.gitpulse.domain.file;

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
        name = "repository_files",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_repo_files_repo_path",
                        columnNames = {"repository_id", "file_path"}
                )
        }
)
public class RepositoryFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repository_id", nullable = false)
    private Repository repository;

    @Column(name = "file_path", length = 1000, nullable = false)
    private String filePath;

    @Column(name = "file_name", length = 255, nullable = false)
    private String fileName;

    @Column(name = "extension", length = 50)
    private String extension;

    @Column(name = "directory_path", length = 1000)
    private String directoryPath;

    @Column(name = "total_revisions", nullable = false)
    private int totalRevisions;

    @Column(name = "total_additions", nullable = false)
    private int totalAdditions;

    @Column(name = "total_deletions", nullable = false)
    private int totalDeletions;

    @Column(name = "total_churn", nullable = false)
    private int totalChurn;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @Column(name = "first_modified_at", nullable = false)
    private Instant firstModifiedAt;

    @Column(name = "last_modified_at", nullable = false)
    private Instant lastModifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_contributor_id")
    private Contributor primaryContributor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RepositoryFile() {
        // Required by JPA
    }

    public RepositoryFile(Repository repository,
                          String filePath,
                          String fileName,
                          String extension,
                          String directoryPath,
                          int totalRevisions,
                          int totalAdditions,
                          int totalDeletions,
                          int totalChurn,
                          boolean isDeleted,
                          Instant firstModifiedAt,
                          Instant lastModifiedAt,
                          Contributor primaryContributor) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.filePath = Objects.requireNonNull(filePath, "filePath must not be null");
        this.fileName = Objects.requireNonNull(fileName, "fileName must not be null");
        this.extension = extension;
        this.directoryPath = directoryPath;
        this.totalRevisions = totalRevisions;
        this.totalAdditions = totalAdditions;
        this.totalDeletions = totalDeletions;
        this.totalChurn = totalChurn;
        this.isDeleted = isDeleted;
        this.firstModifiedAt = Objects.requireNonNull(firstModifiedAt, "firstModifiedAt must not be null");
        this.lastModifiedAt = Objects.requireNonNull(lastModifiedAt, "lastModifiedAt must not be null");
        this.primaryContributor = primaryContributor;
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

    public void updateMetrics(int totalRevisions,
                              int totalAdditions,
                              int totalDeletions,
                              int totalChurn,
                              boolean isDeleted,
                              Instant firstModifiedAt,
                              Instant lastModifiedAt,
                              Contributor primaryContributor) {
        this.totalRevisions = totalRevisions;
        this.totalAdditions = totalAdditions;
        this.totalDeletions = totalDeletions;
        this.totalChurn = totalChurn;
        this.isDeleted = isDeleted;
        if (firstModifiedAt != null) {
            this.firstModifiedAt = firstModifiedAt;
        }
        if (lastModifiedAt != null) {
            this.lastModifiedAt = lastModifiedAt;
        }
        this.primaryContributor = primaryContributor;
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

    public String getFilePath() {
        return filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public String getExtension() {
        return extension;
    }

    public String getDirectoryPath() {
        return directoryPath;
    }

    public int getTotalRevisions() {
        return totalRevisions;
    }

    public int getTotalAdditions() {
        return totalAdditions;
    }

    public int getTotalDeletions() {
        return totalDeletions;
    }

    public int getTotalChurn() {
        return totalChurn;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public Instant getFirstModifiedAt() {
        return firstModifiedAt;
    }

    public Instant getLastModifiedAt() {
        return lastModifiedAt;
    }

    public Contributor getPrimaryContributor() {
        return primaryContributor;
    }

    public void setPrimaryContributor(Contributor primaryContributor) {
        this.primaryContributor = primaryContributor;
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
        if (!(o instanceof RepositoryFile that)) return false;
        return Objects.equals(repository != null ? repository.getId() : null, that.repository != null ? that.repository.getId() : null)
                && Objects.equals(filePath, that.filePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(repository != null ? repository.getId() : null, filePath);
    }

    @Override
    public String toString() {
        return "RepositoryFile{" +
                "id=" + id +
                ", repositoryId=" + (repository != null ? repository.getId() : null) +
                ", filePath='" + filePath + '\'' +
                ", totalRevisions=" + totalRevisions +
                ", totalChurn=" + totalChurn +
                ", isDeleted=" + isDeleted +
                '}';
    }
}