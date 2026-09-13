package com.gitpulse.domain.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitpulse.domain.repository.dto.CreateRepositoryRequest;
import com.gitpulse.integration.github.client.GitHubRepositoryClient;
import com.gitpulse.integration.github.dto.GitHubRepositoryResponse;
import com.gitpulse.integration.github.exception.GitHubRateLimitExceededException;
import com.gitpulse.integration.github.exception.GitHubResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RepositoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RepositoryJpaRepository repositoryJpaRepository;

    @MockBean
    private GitHubRepositoryClient gitHubRepositoryClient;

    @Test
    @DisplayName("POST /api/v1/repositories - Should return 201 Created for valid repository")
    void createRepository_Valid_Returns201() throws Exception {
        CreateRepositoryRequest request = new CreateRepositoryRequest("torvalds", "linux", "Linux kernel source tree", "master");

        mockMvc.perform(post("/api/v1/repositories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.owner", is("torvalds")))
                .andExpect(jsonPath("$.name", is("linux")))
                .andExpect(jsonPath("$.fullName", is("torvalds/linux")))
                .andExpect(jsonPath("$.defaultBranch", is("master")))
                .andExpect(jsonPath("$.description", is("Linux kernel source tree")))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/repositories - Should return 400 Bad Request when owner or name is blank")
    void createRepository_BlankFields_Returns400() throws Exception {
        CreateRepositoryRequest request = new CreateRepositoryRequest("", "  ");

        mockMvc.perform(post("/api/v1/repositories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.validationErrors.owner", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.name", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/repositories - Should return 409 Conflict when repository already registered")
    void createRepository_Duplicate_Returns409() throws Exception {
        Repository existing = new Repository("facebook", "react", "A JavaScript library for building user interfaces", "main");
        repositoryJpaRepository.save(existing);

        CreateRepositoryRequest request = new CreateRepositoryRequest("facebook", "react");

        mockMvc.perform(post("/api/v1/repositories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", is("Repository already exists with fullName: 'facebook/react'")));
    }

    @Test
    @DisplayName("POST /api/v1/repositories/{id}/sync - Should return 200 OK with updated GitHub metadata")
    void syncRepository_Success_Returns200() throws Exception {
        Repository saved = repositoryJpaRepository.save(new Repository("elastic", "elasticsearch", "Search engine", "main"));

        GitHubRepositoryResponse githubData = new GitHubRepositoryResponse();
        githubData.setId(507775L);
        githubData.setDescription("Free and Open, Distributed, RESTful Search Engine");
        githubData.setDefaultBranch("main");
        githubData.setHtmlUrl("https://github.com/elastic/elasticsearch");
        githubData.setLanguage("Java");
        githubData.setIsPrivate(false);
        githubData.setStargazersCount(69000);
        githubData.setForksCount(24000);
        githubData.setOpenIssuesCount(2100);
        githubData.setPushedAt(Instant.parse("2026-09-10T12:00:00Z"));

        when(gitHubRepositoryClient.getRepository("elastic", "elasticsearch")).thenReturn(githubData);

        mockMvc.perform(post("/api/v1/repositories/{id}/sync", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(saved.getId().intValue())))
                .andExpect(jsonPath("$.githubId", is(507775)))
                .andExpect(jsonPath("$.description", is("Free and Open, Distributed, RESTful Search Engine")))
                .andExpect(jsonPath("$.htmlUrl", is("https://github.com/elastic/elasticsearch")))
                .andExpect(jsonPath("$.primaryLanguage", is("Java")))
                .andExpect(jsonPath("$.starsCount", is(69000)))
                .andExpect(jsonPath("$.forksCount", is(24000)))
                .andExpect(jsonPath("$.openIssuesCount", is(2100)));
    }

    @Test
    @DisplayName("POST /api/v1/repositories/{id}/sync - Should return 404 Not Found when local repository does not exist")
    void syncRepository_LocalNotFound_Returns404() throws Exception {
        mockMvc.perform(post("/api/v1/repositories/{id}/sync", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Repository not found with id: '999999'")));
    }

    @Test
    @DisplayName("POST /api/v1/repositories/{id}/sync - Should return 404 Not Found when remote GitHub repository does not exist")
    void syncRepository_GitHubNotFound_Returns404() throws Exception {
        Repository saved = repositoryJpaRepository.save(new Repository("nonexistent-owner", "nonexistent-repo"));

        when(gitHubRepositoryClient.getRepository("nonexistent-owner", "nonexistent-repo"))
                .thenThrow(new GitHubResourceNotFoundException("nonexistent-owner", "nonexistent-repo"));

        mockMvc.perform(post("/api/v1/repositories/{id}/sync", saved.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("GitHub repository not found: 'nonexistent-owner/nonexistent-repo'")));
    }

    @Test
    @DisplayName("POST /api/v1/repositories/{id}/sync - Should return 429 Too Many Requests when rate limited")
    void syncRepository_RateLimitExceeded_Returns429() throws Exception {
        Repository saved = repositoryJpaRepository.save(new Repository("octocat", "Spoon-Knife"));

        when(gitHubRepositoryClient.getRepository("octocat", "Spoon-Knife"))
                .thenThrow(new GitHubRateLimitExceededException("GitHub API rate limit exceeded", 0, 1700000000L));

        mockMvc.perform(post("/api/v1/repositories/{id}/sync", saved.getId()))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status", is(429)))
                .andExpect(jsonPath("$.error", is("Too Many Requests")));
    }

    @Test
    @DisplayName("GET /api/v1/repositories/{id} - Should return 200 OK for existing repository")
    void getRepositoryById_Found_Returns200() throws Exception {
        Repository saved = repositoryJpaRepository.save(new Repository("golang", "go", "The Go programming language", "master"));

        mockMvc.perform(get("/api/v1/repositories/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(saved.getId().intValue())))
                .andExpect(jsonPath("$.owner", is("golang")))
                .andExpect(jsonPath("$.name", is("go")))
                .andExpect(jsonPath("$.fullName", is("golang/go")));
    }

    @Test
    @DisplayName("GET /api/v1/repositories/{id} - Should return 404 Not Found for nonexistent repository")
    void getRepositoryById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/v1/repositories/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is("Repository not found with id: '999999'")));
    }

    @Test
    @DisplayName("GET /api/v1/repositories - Should return 200 OK with repository list")
    void getAllRepositories_Returns200() throws Exception {
        repositoryJpaRepository.save(new Repository("apache", "kafka", "Apache Kafka", "trunk"));

        mockMvc.perform(get("/api/v1/repositories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }
}
