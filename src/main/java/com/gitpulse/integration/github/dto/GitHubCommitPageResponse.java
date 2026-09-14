package com.gitpulse.integration.github.dto;

import java.util.Collections;
import java.util.List;

public class GitHubCommitPageResponse {

    private final List<GitHubCommitResponse> commits;
    private final boolean hasNextPage;

    public GitHubCommitPageResponse(List<GitHubCommitResponse> commits, boolean hasNextPage) {
        this.commits = commits != null ? Collections.unmodifiableList(commits) : Collections.emptyList();
        this.hasNextPage = hasNextPage;
    }

    public List<GitHubCommitResponse> getCommits() {
        return commits;
    }

    public boolean hasNextPage() {
        return hasNextPage;
    }
}
