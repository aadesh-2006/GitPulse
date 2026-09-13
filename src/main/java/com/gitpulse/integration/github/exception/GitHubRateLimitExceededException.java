package com.gitpulse.integration.github.exception;

public class GitHubRateLimitExceededException extends GitHubApiException {

    private final Long resetTimeEpochSeconds;
    private final Integer remainingLimit;

    public GitHubRateLimitExceededException(String message, Integer remainingLimit, Long resetTimeEpochSeconds) {
        super(message, 429);
        this.remainingLimit = remainingLimit;
        this.resetTimeEpochSeconds = resetTimeEpochSeconds;
    }

    public Long getResetTimeEpochSeconds() {
        return resetTimeEpochSeconds;
    }

    public Integer getRemainingLimit() {
        return remainingLimit;
    }
}
