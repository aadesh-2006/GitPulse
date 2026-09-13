package com.gitpulse.integration.github.exception;

public class GitHubResourceNotFoundException extends GitHubApiException {

    private final String owner;
    private final String repo;

    public GitHubResourceNotFoundException(String owner, String repo) {
        super(String.format("GitHub repository not found: '%s/%s'", owner, repo), 404);
        this.owner = owner;
        this.repo = repo;
    }

    public String getOwner() {
        return owner;
    }

    public String getRepo() {
        return repo;
    }
}
