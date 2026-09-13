package com.gitpulse.integration.github.exception;

import com.gitpulse.common.exception.AppException;

public class GitHubApiException extends AppException {

    private final int statusCode;

    public GitHubApiException(String message) {
        super(message);
        this.statusCode = 500;
    }

    public GitHubApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public GitHubApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 500;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
