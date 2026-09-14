package com.gitpulse.integration.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubCommitDetailResponse {

    @JsonProperty("sha")
    private String sha;

    @JsonProperty("html_url")
    private String htmlUrl;

    @JsonProperty("commit")
    private GitHubCommitResponse.CommitDetails commit;

    @JsonProperty("author")
    private GitHubCommitResponse.GitHubUser author;

    @JsonProperty("committer")
    private GitHubCommitResponse.GitHubUser committer;

    @JsonProperty("stats")
    private GitHubCommitResponse.CommitStats stats;

    @JsonProperty("files")
    private List<GitHubFileResponse> files;

    public GitHubCommitDetailResponse() {
    }

    public GitHubCommitDetailResponse(String sha,
                                      String htmlUrl,
                                      GitHubCommitResponse.CommitDetails commit,
                                      GitHubCommitResponse.GitHubUser author,
                                      GitHubCommitResponse.GitHubUser committer,
                                      GitHubCommitResponse.CommitStats stats,
                                      List<GitHubFileResponse> files) {
        this.sha = sha;
        this.htmlUrl = htmlUrl;
        this.commit = commit;
        this.author = author;
        this.committer = committer;
        this.stats = stats;
        this.files = files;
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

    public GitHubCommitResponse.CommitDetails getCommit() {
        return commit;
    }

    public void setCommit(GitHubCommitResponse.CommitDetails commit) {
        this.commit = commit;
    }

    public GitHubCommitResponse.GitHubUser getAuthor() {
        return author;
    }

    public void setAuthor(GitHubCommitResponse.GitHubUser author) {
        this.author = author;
    }

    public GitHubCommitResponse.GitHubUser getCommitter() {
        return committer;
    }

    public void setCommitter(GitHubCommitResponse.GitHubUser committer) {
        this.committer = committer;
    }

    public GitHubCommitResponse.CommitStats getStats() {
        return stats;
    }

    public void setStats(GitHubCommitResponse.CommitStats stats) {
        this.stats = stats;
    }

    public List<GitHubFileResponse> getFiles() {
        return files != null ? files : Collections.emptyList();
    }

    public void setFiles(List<GitHubFileResponse> files) {
        this.files = files;
    }
}
