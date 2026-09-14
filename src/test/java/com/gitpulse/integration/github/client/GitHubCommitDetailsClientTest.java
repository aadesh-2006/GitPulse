package com.gitpulse.integration.github.client;

import com.gitpulse.integration.github.config.GitHubProperties;
import com.gitpulse.integration.github.dto.GitHubCommitDetailResponse;
import com.gitpulse.integration.github.dto.GitHubFileResponse;
import com.gitpulse.integration.github.exception.GitHubAuthenticationException;
import com.gitpulse.integration.github.exception.GitHubRateLimitExceededException;
import com.gitpulse.integration.github.exception.GitHubResourceNotFoundException;
import com.gitpulse.integration.github.exception.GitHubServerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GitHubCommitDetailsClientTest {

    private MockRestServiceServer mockServer;
    private GitHubCommitDetailsClient gitHubCommitDetailsClient;

    @BeforeEach
    void setUp() {
        GitHubProperties properties = new GitHubProperties();
        properties.setBaseUrl("https://api.github.com");
        properties.setToken(null);

        RestClient.Builder restClientBuilder = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .defaultHeader("User-Agent", "GitPulse");

        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        RestClient restClient = restClientBuilder.build();
        gitHubCommitDetailsClient = new GitHubCommitDetailsClient(restClient);
    }

    @Test
    @DisplayName("Should successfully parse commit detail response with multiple changed files")
    void getCommitDetailsSuccess() {
        String json = """
                {
                  "sha": "abc1234567890123456789012345678901234567",
                  "html_url": "https://github.com/octocat/Hello-World/commit/abc1234567890123456789012345678901234567",
                  "commit": {
                    "message": "Add new feature"
                  },
                  "stats": {
                    "total": 15,
                    "additions": 10,
                    "deletions": 5
                  },
                  "files": [
                    {
                      "filename": "src/Main.java",
                      "status": "modified",
                      "additions": 8,
                      "deletions": 2,
                      "changes": 10,
                      "blob_url": "https://github.com/octocat/Hello-World/blob/abc/src/Main.java",
                      "raw_url": "https://github.com/octocat/Hello-World/raw/abc/src/Main.java"
                    },
                    {
                      "filename": "README.md",
                      "status": "added",
                      "additions": 2,
                      "deletions": 3,
                      "changes": 5,
                      "blob_url": "https://github.com/octocat/Hello-World/blob/abc/README.md",
                      "raw_url": "https://github.com/octocat/Hello-World/raw/abc/README.md"
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo("https://api.github.com/repos/octocat/Hello-World/commits/abc1234567890123456789012345678901234567"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(headerDoesNotExist("Authorization"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        GitHubCommitDetailResponse detail = gitHubCommitDetailsClient.getCommitDetails(
                "octocat",
                "Hello-World",
                "abc1234567890123456789012345678901234567"
        );

        mockServer.verify();
        assertThat(detail).isNotNull();
        assertThat(detail.getSha()).isEqualTo("abc1234567890123456789012345678901234567");
        assertThat(detail.getStats().getTotal()).isEqualTo(15);
        assertThat(detail.getFiles()).hasSize(2);

        GitHubFileResponse f1 = detail.getFiles().get(0);
        assertThat(f1.getFilename()).isEqualTo("src/Main.java");
        assertThat(f1.getStatus()).isEqualTo("modified");
        assertThat(f1.getAdditions()).isEqualTo(8);
        assertThat(f1.getDeletions()).isEqualTo(2);
        assertThat(f1.getChanges()).isEqualTo(10);
        assertThat(f1.getBlobUrl()).isEqualTo("https://github.com/octocat/Hello-World/blob/abc/src/Main.java");
        assertThat(f1.getRawUrl()).isEqualTo("https://github.com/octocat/Hello-World/raw/abc/src/Main.java");

        GitHubFileResponse f2 = detail.getFiles().get(1);
        assertThat(f2.getFilename()).isEqualTo("README.md");
        assertThat(f2.getStatus()).isEqualTo("added");
    }

    @Test
    @DisplayName("Should handle renamed files with previous_filename")
    void getCommitDetailsRenamedFile() {
        String json = """
                {
                  "sha": "rename123",
                  "files": [
                    {
                      "filename": "new/path/File.java",
                      "previous_filename": "old/path/File.java",
                      "status": "renamed",
                      "additions": 0,
                      "deletions": 0,
                      "changes": 0
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo("https://api.github.com/repos/octocat/Hello-World/commits/rename123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        GitHubCommitDetailResponse detail = gitHubCommitDetailsClient.getCommitDetails("octocat", "Hello-World", "rename123");

        mockServer.verify();
        assertThat(detail.getFiles()).hasSize(1);
        GitHubFileResponse file = detail.getFiles().get(0);
        assertThat(file.getFilename()).isEqualTo("new/path/File.java");
        assertThat(file.getPreviousFilename()).isEqualTo("old/path/File.java");
        assertThat(file.getStatus()).isEqualTo("renamed");
    }

    @Test
    @DisplayName("Should handle empty files array gracefully")
    void getCommitDetailsEmptyFiles() {
        String json = """
                {
                  "sha": "merge_commit_sha",
                  "files": []
                }
                """;

        mockServer.expect(requestTo("https://api.github.com/repos/octocat/Hello-World/commits/merge_commit_sha"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        GitHubCommitDetailResponse detail = gitHubCommitDetailsClient.getCommitDetails("octocat", "Hello-World", "merge_commit_sha");

        mockServer.verify();
        assertThat(detail.getFiles()).isEmpty();
    }

    @Test
    @DisplayName("Should handle null files field safely")
    void getCommitDetailsNullFiles() {
        String json = """
                {
                  "sha": "bare_sha"
                }
                """;

        mockServer.expect(requestTo("https://api.github.com/repos/octocat/Hello-World/commits/bare_sha"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        GitHubCommitDetailResponse detail = gitHubCommitDetailsClient.getCommitDetails("octocat", "Hello-World", "bare_sha");

        mockServer.verify();
        assertThat(detail.getFiles()).isEmpty();
    }

    @Test
    @DisplayName("HTTP 404 should throw GitHubResourceNotFoundException")
    void getCommitDetailsNotFound() {
        mockServer.expect(requestTo("https://api.github.com/repos/octocat/Hello-World/commits/unknown_sha"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> gitHubCommitDetailsClient.getCommitDetails("octocat", "Hello-World", "unknown_sha"))
                .isInstanceOf(GitHubResourceNotFoundException.class);

        mockServer.verify();
    }

    @Test
    @DisplayName("HTTP 401 should throw GitHubAuthenticationException")
    void getCommitDetailsUnauthorized() {
        mockServer.expect(requestTo("https://api.github.com/repos/octocat/secret-repo/commits/sha123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> gitHubCommitDetailsClient.getCommitDetails("octocat", "secret-repo", "sha123"))
                .isInstanceOf(GitHubAuthenticationException.class);

        mockServer.verify();
    }

    @Test
    @DisplayName("HTTP 403 with rate limit headers should throw GitHubRateLimitExceededException")
    void getCommitDetailsRateLimit403() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("x-ratelimit-remaining", "0");
        headers.add("x-ratelimit-reset", "1700000000");

        mockServer.expect(requestTo("https://api.github.com/repos/octocat/Hello-World/commits/sha123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).headers(headers));

        assertThatThrownBy(() -> gitHubCommitDetailsClient.getCommitDetails("octocat", "Hello-World", "sha123"))
                .isInstanceOf(GitHubRateLimitExceededException.class);

        mockServer.verify();
    }

    @Test
    @DisplayName("HTTP 429 should throw GitHubRateLimitExceededException")
    void getCommitDetailsRateLimit429() {
        mockServer.expect(requestTo("https://api.github.com/repos/octocat/Hello-World/commits/sha123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> gitHubCommitDetailsClient.getCommitDetails("octocat", "Hello-World", "sha123"))
                .isInstanceOf(GitHubRateLimitExceededException.class);

        mockServer.verify();
    }

    @Test
    @DisplayName("HTTP 500 should throw GitHubServerException")
    void getCommitDetailsServerError() {
        mockServer.expect(requestTo("https://api.github.com/repos/octocat/Hello-World/commits/sha123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> gitHubCommitDetailsClient.getCommitDetails("octocat", "Hello-World", "sha123"))
                .isInstanceOf(GitHubServerException.class);

        mockServer.verify();
    }
}
