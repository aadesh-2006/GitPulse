package com.gitpulse.integration.github.client;

import com.gitpulse.integration.github.dto.GitHubCommitPageResponse;
import com.gitpulse.integration.github.dto.GitHubCommitResponse;
import com.gitpulse.integration.github.exception.GitHubApiException;
import com.gitpulse.integration.github.exception.GitHubAuthenticationException;
import com.gitpulse.integration.github.exception.GitHubRateLimitExceededException;
import com.gitpulse.integration.github.exception.GitHubResourceNotFoundException;
import com.gitpulse.integration.github.exception.GitHubServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

@Component
public class GitHubCommitClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubCommitClient.class);
    private static final ParameterizedTypeReference<List<GitHubCommitResponse>> COMMIT_LIST_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public GitHubCommitClient(RestClient gitHubRestClient) {
        this.restClient = gitHubRestClient;
    }

    public GitHubCommitPageResponse getCommitsPage(String owner, String repo, int page, int perPage) {
        log.debug("Fetching commits page for {}/{}: page={}, perPage={}", owner, repo, page, perPage);

        try {
            ResponseEntity<List<GitHubCommitResponse>> responseEntity = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repos/{owner}/{repo}/commits")
                            .queryParam("page", page)
                            .queryParam("per_page", perPage)
                            .build(owner, repo))
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
                    .toEntity(COMMIT_LIST_TYPE);

            List<GitHubCommitResponse> commits = responseEntity.getBody();
            if (commits == null || commits.isEmpty()) {
                return new GitHubCommitPageResponse(Collections.emptyList(), false);
            }

            HttpHeaders headers = responseEntity.getHeaders();
            String linkHeader = headers.getFirst(HttpHeaders.LINK);
            boolean hasNextPage = parseHasNextPage(linkHeader);

            return new GitHubCommitPageResponse(commits, hasNextPage);

        } catch (ResourceAccessException ex) {
            log.error("Network or timeout error communicating with GitHub API for {}/{}: {}", owner, repo, ex.getMessage());
            throw new GitHubApiException("Timeout or network error communicating with GitHub API: " + ex.getMessage(), ex);
        }
    }

    public static boolean parseHasNextPage(String linkHeader) {
        if (linkHeader == null || linkHeader.isBlank()) {
            return false;
        }

        String[] links = linkHeader.split(",");
        for (String link : links) {
            String[] segments = link.split(";");
            if (segments.length >= 2) {
                for (int i = 1; i < segments.length; i++) {
                    String param = segments[i].trim().toLowerCase();
                    if (param.equals("rel=\"next\"") || param.equals("rel='next'") || param.equals("rel=next")) {
                        return true;
                    }
                }
            }
        }
        return false;
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
