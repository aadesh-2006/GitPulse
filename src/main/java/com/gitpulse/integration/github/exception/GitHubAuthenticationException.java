package com.gitpulse.integration.github.exception;

public class GitHubAuthenticationException extends GitHubApiException {

    public GitHubAuthenticationException(String message) {
        super(message, 401);
    }
}
