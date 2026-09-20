package com.gitpulse.common;

import com.gitpulse.domain.commit.Commit;
import com.gitpulse.domain.commit.CommitClassification;
import com.gitpulse.domain.commit.CommitJpaRepository;
import com.gitpulse.domain.repository.Repository;
import com.gitpulse.domain.repository.RepositoryJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiSecurityHardeningIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RepositoryJpaRepository repositoryJpaRepository;

    @Autowired
    private CommitJpaRepository commitJpaRepository;

    private Repository repo1;
    private Repository repo2;
    private Commit commit1;

    @BeforeEach
    void setUp() {
        repo1 = repositoryJpaRepository.save(new Repository("owner-a", "repo-a"));
        repo2 = repositoryJpaRepository.save(new Repository("owner-b", "repo-b"));

        commit1 = commitJpaRepository.save(new Commit(
                repo1,
                "sha-1234567890abcdef",
                "feat: initial feature",
                "Alice",
                "alice@example.com",
                "alice",
                Instant.parse("2026-01-15T10:00:00Z"),
                10,
                2,
                12,
                null,
                CommitClassification.FEATURE
        ));
    }

    @Test
    @DisplayName("POST /api/v1/repositories with blank or invalid fields should return 400 Bad Request with structured errors")
    void testCreateRepositoryValidationErrors() throws Exception {
        mockMvc.perform(post("/api/v1/repositories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "owner": "",
                                  "name": "invalid name with spaces!"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.validationErrors.owner").exists())
                .andExpect(jsonPath("$.validationErrors.name").exists());
    }

    @Test
    @DisplayName("GET /api/v1/repositories/{id} with non-numeric ID should return 400 Bad Request")
    void testGetRepositoryWithInvalidIdType() throws Exception {
        mockMvc.perform(get("/api/v1/repositories/invalid-id")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Invalid parameter value for: id")));
    }

    @Test
    @DisplayName("GET /api/v1/repositories/{id}/commits with invalid sort field should return 400 Bad Request")
    void testGetCommitsInvalidSortField() throws Exception {
        mockMvc.perform(get("/api/v1/repositories/" + repo1.getId() + "/commits")
                        .param("sort", "maliciousColumn,desc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("GET /api/v1/repositories/{id}/commits with from after to should return 400 Bad Request")
    void testGetCommitsFromAfterTo() throws Exception {
        mockMvc.perform(get("/api/v1/repositories/" + repo1.getId() + "/commits")
                        .param("from", "2026-03-01T00:00:00Z")
                        .param("to", "2026-01-01T00:00:00Z")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("The 'from' timestamp must be strictly before the 'to' timestamp")));
    }

    @Test
    @DisplayName("GET /api/v1/repositories/{id}/commits with invalid classification enum should return 400 Bad Request")
    void testGetCommitsInvalidEnum() throws Exception {
        mockMvc.perform(get("/api/v1/repositories/" + repo1.getId() + "/commits")
                        .param("classification", "NOT_A_VALID_CLASSIFICATION")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Invalid parameter value for: classification")));
    }

    @Test
    @DisplayName("GET /api/v1/repositories/{id}/commits with invalid date format should return 400 Bad Request")
    void testGetCommitsInvalidDateFormat() throws Exception {
        mockMvc.perform(get("/api/v1/repositories/" + repo1.getId() + "/commits")
                        .param("from", "not-a-valid-date")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Invalid parameter value for: from")));
    }

    @Test
    @DisplayName("GET /api/v1/repositories/{id}/contributors with invalid sort field should return 400 Bad Request")
    void testGetContributorsInvalidSortField() throws Exception {
        mockMvc.perform(get("/api/v1/repositories/" + repo1.getId() + "/contributors")
                        .param("sort", "unknownField,desc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("GET /api/v1/repositories/{id}/files with invalid sort field should return 400 Bad Request")
    void testGetFilesInvalidSortField() throws Exception {
        mockMvc.perform(get("/api/v1/repositories/" + repo1.getId() + "/files")
                        .param("sort", "hacked_column,desc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("GET /api/v1/repositories/{id}/files with oversized size should bound page size to MAX_PAGE_SIZE")
    void testGetFilesOversizedPageSizeClamped() throws Exception {
        mockMvc.perform(get("/api/v1/repositories/" + repo1.getId() + "/files")
                        .param("size", "500")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size", is(100)));
    }

    @Test
    @DisplayName("GET /api/v1/repositories/{id}/commits with oversized size should bound page size to MAX_PAGE_SIZE")
    void testGetCommitsOversizedPageSizeClamped() throws Exception {
        mockMvc.perform(get("/api/v1/repositories/" + repo1.getId() + "/commits")
                        .param("size", "500")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size", is(100)));
    }

    @Test
    @DisplayName("GET /api/v1/repositories/{id}/contributor-files with invalid sort field should return 400 Bad Request")
    void testGetContributorFilesInvalidSortField() throws Exception {
        mockMvc.perform(get("/api/v1/repositories/" + repo1.getId() + "/contributor-files")
                        .param("sort", "nonexistent,asc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("GET /api/v1/repositories/{id}/file-ownership with invalid sort field should return 400 Bad Request")
    void testGetFileOwnershipInvalidSortField() throws Exception {
        mockMvc.perform(get("/api/v1/repositories/" + repo1.getId() + "/file-ownership")
                        .param("sort", "invalidColumn,desc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("POST /api/v1/repositories with malformed JSON body should return 400 Bad Request")
    void testMalformedJsonRequestBody() throws Exception {
        mockMvc.perform(post("/api/v1/repositories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{malformed-json:"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Malformed or unreadable request body")));
    }

    @Test
    @DisplayName("DELETE /api/v1/repositories (unsupported HTTP method) should return 405 Method Not Allowed")
    void testUnsupportedHttpMethod() throws Exception {
        mockMvc.perform(delete("/api/v1/repositories"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status", is(405)));
    }

    @Test
    @DisplayName("GET /api/v1/nonexistent-route should return 404 Not Found")
    void testUnknownRoute() throws Exception {
        mockMvc.perform(get("/api/v1/nonexistent-route")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @DisplayName("Cross-repository resource isolation: accessing commit belonging to Repo A via Repo B URL should return 404 Not Found")
    void testCrossRepositoryIsolation() throws Exception {
        mockMvc.perform(get("/api/v1/repositories/" + repo2.getId() + "/commits/" + commit1.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }
}
