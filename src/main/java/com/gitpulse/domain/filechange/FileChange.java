package com.gitpulse.domain.filechange;

import com.gitpulse.domain.commit.Commit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "file_changes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_file_changes_commit_file",
                        columnNames = {"commit_id", "file_path"}
                )
        }
)
public class FileChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "commit_id", nullable = false)
    private Commit commit;

    @Column(name = "file_path", length = 500, nullable = false)
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    private FileChangeStatus status;

    @Column(name = "additions")
    private Integer additions;

    @Column(name = "deletions")
    private Integer deletions;

    @Column(name = "changes")
    private Integer changes;

    @Column(name = "blob_url", length = 500)
    private String blobUrl;

    @Column(name = "raw_url", length = 500)
    private String rawUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FileChange() {
        // JPA standard default constructor
    }

    public FileChange(Commit commit,
                      String filePath,
                      FileChangeStatus status,
                      Integer additions,
                      Integer deletions,
                      Integer changes,
                      String blobUrl,
                      String rawUrl) {
        this.commit = Objects.requireNonNull(commit, "commit must not be null");
        this.filePath = truncate(Objects.requireNonNull(filePath, "filePath must not be null").trim(), 500);
        this.status = status != null ? status : FileChangeStatus.UNKNOWN;
        this.additions = additions;
        this.deletions = deletions;
        this.changes = changes;
        this.blobUrl = truncate(blobUrl, 500);
        this.rawUrl = truncate(rawUrl, 500);
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.status == null) {
            this.status = FileChangeStatus.UNKNOWN;
        }
    }

    public Long getId() {
        return id;
    }

    public Commit getCommit() {
        return commit;
    }

    public void setCommit(Commit commit) {
        this.commit = commit;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = truncate(filePath, 500);
    }

    public FileChangeStatus getStatus() {
        return status;
    }

    public void setStatus(FileChangeStatus status) {
        this.status = status != null ? status : FileChangeStatus.UNKNOWN;
    }

    public Integer getAdditions() {
        return additions;
    }

    public void setAdditions(Integer additions) {
        this.additions = additions;
    }

    public Integer getDeletions() {
        return deletions;
    }

    public void setDeletions(Integer deletions) {
        this.deletions = deletions;
    }

    public Integer getChanges() {
        return changes;
    }

    public void setChanges(Integer changes) {
        this.changes = changes;
    }

    public String getBlobUrl() {
        return blobUrl;
    }

    public void setBlobUrl(String blobUrl) {
        this.blobUrl = truncate(blobUrl, 500);
    }

    public String getRawUrl() {
        return rawUrl;
    }

    public void setRawUrl(String rawUrl) {
        this.rawUrl = truncate(rawUrl, 500);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    private static String truncate(String val, int maxLen) {
        if (val == null) {
            return null;
        }
        val = val.trim();
        return val.length() > maxLen ? val.substring(0, maxLen) : val;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileChange that)) return false;
        return Objects.equals(commit != null ? commit.getId() : null, that.commit != null ? that.commit.getId() : null)
                && Objects.equals(filePath, that.filePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commit != null ? commit.getId() : null, filePath);
    }

    @Override
    public String toString() {
        return "FileChange{" +
                "id=" + id +
                ", filePath='" + filePath + '\'' +
                ", status=" + status +
                ", additions=" + additions +
                ", deletions=" + deletions +
                '}';
    }
}
