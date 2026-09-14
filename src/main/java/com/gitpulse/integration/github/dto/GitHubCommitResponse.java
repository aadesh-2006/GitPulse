package com.gitpulse.integration.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubCommitResponse {

    private String sha;

    @JsonProperty("html_url")
    private String htmlUrl;

    private CommitDetails commit;

    private GitHubUser author;

    private GitHubUser committer;

    private CommitStats stats;

    public GitHubCommitResponse() {
    }

    public GitHubCommitResponse(String sha,
                                String htmlUrl,
                                CommitDetails commit,
                                GitHubUser author,
                                GitHubUser committer,
                                CommitStats stats) {
        this.sha = sha;
        this.htmlUrl = htmlUrl;
        this.commit = commit;
        this.author = author;
        this.committer = committer;
        this.stats = stats;
    }

    public String getSha() {
        return sha;
    }

    public void setSha(String sha) {
        this.sha = sha;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public CommitDetails getCommit() {
        return commit;
    }

    public void setCommit(CommitDetails commit) {
        this.commit = commit;
    }

    public GitHubUser getAuthor() {
        return author;
    }

    public void setAuthor(GitHubUser author) {
        this.author = author;
    }

    public GitHubUser getCommitter() {
        return committer;
    }

    public void setCommitter(GitHubUser committer) {
        this.committer = committer;
    }

    public CommitStats getStats() {
        return stats;
    }

    public void setStats(CommitStats stats) {
        this.stats = stats;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CommitDetails {
        private String message;
        private GitUser author;
        private GitUser committer;

        public CommitDetails() {
        }

        public CommitDetails(String message, GitUser author, GitUser committer) {
            this.message = message;
            this.author = author;
            this.committer = committer;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public GitUser getAuthor() {
            return author;
        }

        public void setAuthor(GitUser author) {
            this.author = author;
        }

        public GitUser getCommitter() {
            return committer;
        }

        public void setCommitter(GitUser committer) {
            this.committer = committer;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GitUser {
        private String name;
        private String email;
        private Instant date;

        public GitUser() {
        }

        public GitUser(String name, String email, Instant date) {
            this.name = name;
            this.email = email;
            this.date = date;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public Instant getDate() {
            return date;
        }

        public void setDate(Instant date) {
            this.date = date;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GitHubUser {
        private String login;
        private Long id;

        public GitHubUser() {
        }

        public GitHubUser(String login, Long id) {
            this.login = login;
            this.id = id;
        }

        public String getLogin() {
            return login;
        }

        public void setLogin(String login) {
            this.login = login;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CommitStats {
        private Integer additions;
        private Integer deletions;
        private Integer total;

        public CommitStats() {
        }

        public CommitStats(Integer additions, Integer deletions, Integer total) {
            this.additions = additions;
            this.deletions = deletions;
            this.total = total;
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

        public Integer getTotal() {
            return total;
        }

        public void setTotal(Integer total) {
            this.total = total;
        }
    }
}
