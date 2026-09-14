package com.gitpulse.integration.github.client;

import com.gitpulse.integration.github.config.GitHubProperties;
import com.gitpulse.integration.github.dto.GitHubCommitPageResponse;
import com.gitpulse.integration.github.dto.GitHubCommitResponse;
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

class GitHubCommitClientTest {

    private MockRestServiceServer mockServer;
    private GitHubCommitClient gitHubCommitClient;
    private RestClient.Builder restClientBuilder;

    @BeforeEach
    void setUp() {
        GitHubProperties properties = new GitHubProperties();
        properties.setBaseUrl("https://api.github.com");
        properties.setToken(null);

        restClientBuilder = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .defaultHeader("User-Agent", "GitPulse");

        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        RestClient restClient = restClientBuilder.build();
        gitHubCommitClient = new GitHubCommitClient(restClient);
    }

    @Test
    @DisplayName("Single page response without Link header should return commits and hasNextPage=false")
    void getCommitsPageSinglePage() {
        String json = """
                [
                  {
                    "sha": "abc1234567890123456789012345678901234567",
                    "html_url": "https://github.com/octocat/Hello-World/commit/abc1234567890123456789012345678901234567",
                    "commit": {
                      "message": "Initial commit",
                      "author": {
                        "name": "Octocat",
                        "email": "octocat@github.com",
                        "date": "2026-09-01T10:00:00Z"
                      },
                      "committer": {
                        "name": "Octocat",
                        "email": "octocat@github.com",
                        "date": "2026-09-01T10:00:00Z"
                      }
                    },
                    "author": {
                      "login": "octocat",
                      "id": 1
                    },
                    "committer": {
                      "login": "octocat",
                      "id": 1
                    },
                    "stats": {
                      "additions": 10,
                      "deletions": 2,
                      "total": 12
                    }
                  }
                ]
                """;

        mockServer.expect(requestTo("https://api.github.com/repos/octocat/Hello-World/commits?page=1&per_page=30"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(headerDoesNotExist("Authorization"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        GitHubCommitPageResponse page = gitHubCommitClient.getCommitsPage("octocat", "Hello-World", 1, 30);

        mockServer.verify();
        assertThat(page.getCommits()).hasSize(1);
        assertThat(page.hasNextPage()).isFalse();

        GitHubCommitResponse commit = page.getCommits().get(0);
        assertThat(commit.getSha()).isEqualTo("abc1234567890123456789012345678901234567");
        assertThat(commit.getHtmlUrl()).isEqualTo("https://github.com/octocat/Hello-World/commit/abc1234567890123456789012345678901234567");
        assertThat(commit.getCommit().getMessage()).isEqualTo("Initial commit");
        assertThat(commit.getCommit().getAuthor().getName()).isEqualTo("Octocat");
        assertThat(commit.getCommit().getAuthor().getEmail()).isEqualTo("octocat@github.com");
        assertThat(commit.getCommit().getAuthor().getDate()).isEqualTo("2026-09-01T10:00:00Z");
        assertThat(commit.getAuthor().getLogin()).isEqualTo("octocat");
        assertThat(commit.getStats().getAdditions()).isEqualTo(10);
        assertThat(commit.getStats().getDeletions()).isEqualTo(2);
        assertThat(commit.getStats().getTotal()).isEqualTo(12);
    }

    @Test
    @DisplayName("Multiple pages with Link header rel=next should indicate hasNextPage=true")
    void getCommitsPageWithLinkHeaderNext() {
        String json = """
                [
                  {
                    "sha": "sha1111",
                    "commit": { "message": "Commit 1" }
                  }
                ]
                """;

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LINK, "<https://api.github.com/repos/octocat/Hello-World/commits?page=2&per_page=30>; rel=\"next\", <https://api.github.com/repos/octocat/Hello-World/commits?page=5&per_page=30>; rel=\"last\"");

        mockServer.expect(requestTo("https://api.github.com/repos/octocat/Hello-World/commits?page=1&per_page=30"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON).headers(headers));

        GitHubCommitPageResponse page = gitHubCommitClient.getCommitsPage("octocat", "Hello-World", 1, 30);

        mockServer.verify();
        assertThat(page.getCommits()).hasSize(1);
        assertThat(page.hasNextPage()).isTrue();
    }

    @Test
    @DisplayName("Last page with Link header having only rel=prev, rel=first should return hasNextPage=false")
    void getCommitsPageLastPageWithoutNext() {
        String json = """
                [
                  {
                    "sha": "sha9999",
                    "commit": { "message": "Final commit" }
                  }
                ]
                """;

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LINK, "<https://api.github.com/repos/octocat/Hello-World/commits?page=1&per_page=30>; rel=\"first\", <https://api.github.com/repos/octocat/Hello-World/commits?page=2&per_page=30>; rel=\"prev\"");

        mockServer.expect(requestTo("https://api.github.com/repos/octocat/Hello-World/commits?page=3&per_page=30"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON).headers(headers));

        GitHubCommitPageResponse page = gitHubCommitClient.getCommitsPage("octocat", "Hello-World", 3, 30);

        mockServer.verify();
        assertThat(page.getCommits()).hasSize(1);
        assertThat(page.hasNextPage()).isFalse();
    }

    @Test
    @DisplayName("Empty commits response should return empty list and hasNextPage=false")
    void getCommitsPageEmpty() {
        mockServer.expect(requestTo("https://api.github.com/repos/octocat/empty-repo/commits?page=1&per_page=30"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        GitHubCommitPageResponse page = gitHubCommitClient.getCommitsPage("octocat", "empty-repo", 1, 30);

        mockServer.verify();
        assertThat(page.getCommits()).isEmpty();
        assertThat(page.hasNextPage()).isFalse();
    }

    @Test
    @DisplayName("Missing GitHub author, committer, and stats should deserialize safely with nulls")
    void getCommitsPageNullFields() {
        String json = """
                [
                  {
                    "sha": "sha_bare",
                    "commit": {
                      "message": "Bare git commit",
                      "author": null,
                      "committer": null
                    },
                    "author": null,
                    "committer": null,
                    "stats": null
                  }
                ]
                """;

        mockServer.expect(requestTo("https://api.github.com/repos/octocat/Hello-World/commits?page=1&per_page=30"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        GitHubCommitPageResponse page = gitHubCommitClient.getCommitsPage("octocat", "Hello-World", 1, 30);

        mockServer.verify();
        assertThat(page.getCommits()).hasSize(1);
        GitHubCommitResponse commit = page.getCommits().get(0);
        assertThat(commit.getSha()).isEqualTo("sha_bare");
        assertThat(commit.getCommit().getMessage()).isEqualTo("Bare git commit");
        assertThat(commit.getCommit().getAuthor()).isNull();
        assertThat(commit.getAuthor()).isNull();
        assertThat(commit.getStats()).isNull();
    }

    @Test
    @DisplayName("HTTP 404 should throw GitHubResourceNotFoundException")
    void getCommitsPageNotFound() {
        mockServer.expect(requestTo("https://api.github.com/repos/octocat/unknown/commits?page=1&per_page=30"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> gitHubCommitClient.getCommitsPage("octocat", "unknown", 1, 30))
                .isInstanceOf(GitHubResourceNotFoundException.class)
                .hasMessageContaining("GitHub repository not found: 'octocat/unknown'");

        mockServer.verify();
    }

    @Test
    @DisplayName("HTTP 401 should throw GitHubAuthenticationException")
    void getCommitsPageUnauthorized() {
        mockServer.expect(requestTo("https://api.github.com/repos/octocat/secret/commits?page=1&per_page=30"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> gitHubCommitClient.getCommitsPage("octocat", "secret", 1, 30))
                .isInstanceOf(GitHubAuthenticationException.class)
                .hasMessageContaining("GitHub authentication failed");

        mockServer.verify();
    }

    @Test
    @DisplayName("HTTP 403 with zero remaining should throw GitHubRateLimitExceededException")
    void getCommitsPageRateLimit403() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("x-ratelimit-remaining", "0");
        headers.add("x-ratelimit-reset", "1700000000");

        mockServer.expect(requestTo("https://api.github.com/repos/octocat/Hello-World/commits?page=1&per_page=30"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).headers(headers));

        assertThatThrownBy(() -> gitHubCommitClient.getCommitsPage("octocat", "Hello-World", 1, 30))
                .isInstanceOf(GitHubRateLimitExceededException.class)
                .hasMessageContaining("rate limit exceeded");

        mockServer.verify();
    }

    @Test
    @DisplayName("HTTP 429 should throw GitHubRateLimitExceededException")
    void getCommitsPageRateLimit429() {
        mockServer.expect(requestTo("https://api.github.com/repos/octocat/Hello-World/commits?page=1&per_page=30"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> gitHubCommitClient.getCommitsPage("octocat", "Hello-World", 1, 30))
                .isInstanceOf(GitHubRateLimitExceededException.class);

        mockServer.verify();
    }

    @Test
    @DisplayName("HTTP 500 should throw GitHubServerException")
    void getCommitsPageServerError() {
        mockServer.expect(requestTo("https://api.github.com/repos/octocat/Hello-World/commits?page=1&per_page=30"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> gitHubCommitClient.getCommitsPage("octocat", "Hello-World", 1, 30))
                .isInstanceOf(GitHubServerException.class);

        mockServer.verify();
    }

    @Test
    @DisplayName("Link header parser should correctly handle multiple links and formats")
    void linkHeaderParsing() {
        assertThat(GitHubCommitClient.parseHasNextPage(null)).isFalse();
        assertThat(GitHubCommitClient.parseHasNextPage("")).isFalse();
        assertThat(GitHubCommitClient.parseHasNextPage("<url>; rel=\"last\"")).isFalse();
        assertThat(GitHubCommitClient.parseHasNextPage("<url>; rel=\"next\"")).isTrue();
        assertThat(GitHubCommitClient.parseHasNextPage("<url1>; rel=\"prev\", <url2>; rel=\"next\", <url3>; rel=\"last\"")).isTrue();
        assertThat(GitHubCommitClient.parseHasNextPage("<url1>; rel=\"prev\", <url2>; rel=\"last\"")).isFalse();
        assertThat(GitHubCommitClient.parseHasNextPage("<url1>; rel=next")).isTrue();
    }
}
