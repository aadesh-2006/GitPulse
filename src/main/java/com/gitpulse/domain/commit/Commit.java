package com.gitpulse.domain.commit;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "commits",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_commits_repo_sha",
                        columnNames = {"repository_id", "github_commit_sha"}
                )
        }
)
public class Commit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repository_id", nullable = false)
    private Repository repository;

    @Column(name = "github_commit_sha", length = 40, nullable = false)
    private String githubCommitSha;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "author_name", length = 200)
    private String authorName;

    @Column(name = "author_email", length = 200)
    private String authorEmail;

    @Column(name = "author_username", length = 100)
    private String authorUsername;

    @Column(name = "committed_at", nullable = false)
    private Instant committedAt;

    @Column(name = "additions")
    private Integer additions;

    @Column(name = "deletions")
    private Integer deletions;

    @Column(name = "total_changes")
    private Integer totalChanges;

    @Column(name = "html_url", length = 300)
    private String htmlUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Commit() {
        // JPA standard default constructor
    }

    public Commit(Repository repository,
                  String githubCommitSha,
                  String message,
                  String authorName,
                  String authorEmail,
                  String authorUsername,
                  Instant committedAt,
                  Integer additions,
                  Integer deletions,
                  Integer totalChanges,
                  String htmlUrl) {
        this.repository = repository;
        this.githubCommitSha = githubCommitSha;
        this.message = message;
        this.authorName = authorName;
        this.authorEmail = authorEmail;
        this.authorUsername = authorUsername;
        this.committedAt = committedAt;
        this.additions = additions;
        this.deletions = deletions;
        this.totalChanges = totalChanges;
        this.htmlUrl = htmlUrl;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
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

    public String getGithubCommitSha() {
        return githubCommitSha;
    }

    public void setGithubCommitSha(String githubCommitSha) {
        this.githubCommitSha = githubCommitSha;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }

    public String getAuthorUsername() {
        return authorUsername;
    }

    public void setAuthorUsername(String authorUsername) {
        this.authorUsername = authorUsername;
    }

    public Instant getCommittedAt() {
        return committedAt;
    }

    public void setCommittedAt(Instant committedAt) {
        this.committedAt = committedAt;
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

    public Integer getTotalChanges() {
        return totalChanges;
    }

    public void setTotalChanges(Integer totalChanges) {
        this.totalChanges = totalChanges;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Commit commit)) return false;
        return Objects.equals(repository != null ? repository.getId() : null, commit.repository != null ? commit.repository.getId() : null)
                && Objects.equals(githubCommitSha, commit.githubCommitSha);
    }

    @Override
    public int hashCode() {
        return Objects.hash(repository != null ? repository.getId() : null, githubCommitSha);
    }

    @Override
    public String toString() {
        return "Commit{" +
                "id=" + id +
                ", githubCommitSha='" + githubCommitSha + '\'' +
                ", authorUsername='" + authorUsername + '\'' +
                ", committedAt=" + committedAt +
                '}';
    }
}
