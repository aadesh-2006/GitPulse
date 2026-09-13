package com.gitpulse.integration.github.exception;

public class GitHubServerException extends GitHubApiException {

    public GitHubServerException(String message, int statusCode) {
        super(message, statusCode);
    }
}
