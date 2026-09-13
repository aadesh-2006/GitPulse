package com.gitpulse.integration.github.client;

import com.gitpulse.integration.github.dto.GitHubRepositoryResponse;
import com.gitpulse.integration.github.exception.GitHubApiException;
import com.gitpulse.integration.github.exception.GitHubAuthenticationException;
import com.gitpulse.integration.github.exception.GitHubRateLimitExceededException;
import com.gitpulse.integration.github.exception.GitHubResourceNotFoundException;
import com.gitpulse.integration.github.exception.GitHubServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class GitHubRepositoryClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubRepositoryClient.class);

    private final RestClient restClient;

    public GitHubRepositoryClient(RestClient gitHubRestClient) {
        this.restClient = gitHubRestClient;
    }

    public GitHubRepositoryResponse getRepository(String owner, String repo) {
        log.info("Fetching repository metadata from GitHub for {}/{}", owner, repo);

        try {
            return restClient.get()
                    .uri("/repos/{owner}/{repo}", owner, repo)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        log.warn("GitHub repository not found: {}/{}", owner, repo);
                        throw new GitHubResourceNotFoundException(owner, repo);
                    })
                    .onStatus(status -> status.value() == 401, (request, response) -> {
                        log.warn("GitHub API authentication failed (401 Unauthorized)");
                        throw new GitHubAuthenticationException("GitHub authentication failed: invalid or expired token");
                    })
                    .onStatus(status -> status.value() == 403 || status.value() == 429, (request, response) -> {
                        String remaining = response.getHeaders().getFirst("x-ratelimit-remaining");
                        String reset = response.getHeaders().getFirst("x-ratelimit-reset");
                        Integer remainingCount = parseInteger(remaining);
                        Long resetEpoch = parseLong(reset);

                        log.warn("GitHub rate limit exceeded or access forbidden: status={}, remaining={}, reset={}",
                                response.getStatusCode(), remaining, reset);
                        throw new GitHubRateLimitExceededException(
                                "GitHub API rate limit exceeded or access forbidden",
                                remainingCount,
                                resetEpoch
                        );
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        log.error("GitHub API server error: status={}", response.getStatusCode());
                        throw new GitHubServerException("GitHub API server error: " + response.getStatusCode(), response.getStatusCode().value());
                    })
                    .toEntity(GitHubRepositoryResponse.class)
                    .getBody();
        } catch (ResourceAccessException ex) {
            log.error("Network or timeout error communicating with GitHub API for {}/{}: {}", owner, repo, ex.getMessage());
            throw new GitHubApiException("Timeout or network error communicating with GitHub API: " + ex.getMessage(), ex);
        }
    }

    private static Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
