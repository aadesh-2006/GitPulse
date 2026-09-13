package com.gitpulse.integration.github.client;

import com.gitpulse.integration.github.config.GitHubClientConfig;
import com.gitpulse.integration.github.config.GitHubProperties;
import com.gitpulse.integration.github.dto.GitHubRepositoryResponse;
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
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GitHubRepositoryClientTest {

    private MockRestServiceServer mockServer;
    private GitHubRepositoryClient gitHubRepositoryClient;
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
        gitHubRepositoryClient = new GitHubRepositoryClient(restClient);
    }

    @Test
    @DisplayName("Should successfully fetch and deserialize repository metadata from GitHub")
    void getRepository_Success_ReturnsMappedDto() {
        String jsonResponse = """
                {
                    "id": 1296269,
                    "name": "Hello-World",
                    "full_name": "octocat/Hello-World",
                    "description": "This your first repo!",
                    "default_branch": "master",
                    "html_url": "https://github.com/octocat/Hello-World",
                    "language": "Java",
                    "private": false,
                    "pushed_at": "2024-01-01T12:00:00Z",
                    "stargazers_count": 1337,
                    "forks_count": 42,
                    "open_issues_count": 5
                }
                """;

        mockServer.expect(requestTo("https://api.github.com/repos/octocat/Hello-World"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
                .andExpect(headerDoesNotExist("Authorization"))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        GitHubRepositoryResponse response = gitHubRepositoryClient.getRepository("octocat", "Hello-World");

        mockServer.verify();

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1296269L);
        assertThat(response.getName()).isEqualTo("Hello-World");
        assertThat(response.getFullName()).isEqualTo("octocat/Hello-World");
        assertThat(response.getDescription()).isEqualTo("This your first repo!");
        assertThat(response.getDefaultBranch()).isEqualTo("master");
        assertThat(response.getHtmlUrl()).isEqualTo("https://github.com/octocat/Hello-World");
        assertThat(response.getLanguage()).isEqualTo("Java");
        assertThat(response.getIsPrivate()).isFalse();
        assertThat(response.getStargazersCount()).isEqualTo(1337);
        assertThat(response.getForksCount()).isEqualTo(42);
        assertThat(response.getOpenIssuesCount()).isEqualTo(5);
        assertThat(response.getPushedAt()).isEqualTo("2024-01-01T12:00:00Z");
    }

    @Test
    @DisplayName("Should include Authorization header when GitHub token is configured")
    void getRepository_WithToken_SendsAuthorizationHeader() {
        RestClient.Builder tokenBuilder = RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .defaultHeader("User-Agent", "GitPulse")
                .defaultHeader("Authorization", "Bearer ghp_testToken12345");

        MockRestServiceServer tokenServer = MockRestServiceServer.bindTo(tokenBuilder).build();
        RestClient tokenClient = tokenBuilder.build();
        GitHubRepositoryClient clientWithToken = new GitHubRepositoryClient(tokenClient);

        tokenServer.expect(requestTo("https://api.github.com/repos/octocat/Hello-World"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer ghp_testToken12345"))
                .andRespond(withSuccess("{\"id\": 1, \"name\": \"Hello-World\"}", MediaType.APPLICATION_JSON));

        GitHubRepositoryResponse response = clientWithToken.getRepository("octocat", "Hello-World");

        tokenServer.verify();
        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should throw GitHubResourceNotFoundException when GitHub returns 404")
    void getRepository_404NotFound_ThrowsException() {
        mockServer.expect(requestTo("https://api.github.com/repos/octocat/unknown-repo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> gitHubRepositoryClient.getRepository("octocat", "unknown-repo"))
                .isInstanceOf(GitHubResourceNotFoundException.class)
                .hasMessageContaining("GitHub repository not found: 'octocat/unknown-repo'");

        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw GitHubAuthenticationException when GitHub returns 401")
    void getRepository_401Unauthorized_ThrowsException() {
        mockServer.expect(requestTo("https://api.github.com/repos/octocat/secret-repo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> gitHubRepositoryClient.getRepository("octocat", "secret-repo"))
                .isInstanceOf(GitHubAuthenticationException.class)
                .hasMessageContaining("GitHub authentication failed");

        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw GitHubRateLimitExceededException when rate limited (403/429)")
    void getRepository_RateLimit_ThrowsException() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("x-ratelimit-remaining", "0");
        headers.add("x-ratelimit-reset", "1700000000");

        mockServer.expect(requestTo("https://api.github.com/repos/octocat/Hello-World"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).headers(headers));

        assertThatThrownBy(() -> gitHubRepositoryClient.getRepository("octocat", "Hello-World"))
                .isInstanceOf(GitHubRateLimitExceededException.class)
                .hasMessageContaining("rate limit exceeded");

        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw GitHubServerException when GitHub returns 5xx")
    void getRepository_500ServerError_ThrowsException() {
        mockServer.expect(requestTo("https://api.github.com/repos/octocat/Hello-World"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        assertThatThrownBy(() -> gitHubRepositoryClient.getRepository("octocat", "Hello-World"))
                .isInstanceOf(GitHubServerException.class)
                .hasMessageContaining("GitHub API server error");

        mockServer.verify();
    }
}
