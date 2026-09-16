package com.gitpulse.domain.contributor;

import com.gitpulse.common.exception.GlobalExceptionHandler;
import com.gitpulse.common.exception.ResourceNotFoundException;
import com.gitpulse.domain.contributor.dto.ContributorResponse;
import com.gitpulse.domain.contributor.dto.RepositoryContributorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContributorController.class)
@Import(GlobalExceptionHandler.class)
class ContributorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContributorService contributorService;

    @Test
    @DisplayName("GET /api/v1/repositories/{id}/contributors should return paginated repository contributors")
    void getRepositoryContributors() throws Exception {
        Instant now = Instant.parse("2026-09-01T12:00:00Z");
        ContributorResponse contributor = new ContributorResponse(10L, "alice@test.com", "alice", "Alice", null, null, now, now);
        RepositoryContributorResponse rc = new RepositoryContributorResponse(100L, 1L, contributor, 25, 100, 20, 120, now, now, now, now);

        when(contributorService.getRepositoryContributors(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(rc), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/repositories/1/contributors")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(100))
                .andExpect(jsonPath("$.content[0].repositoryId").value(1))
                .andExpect(jsonPath("$.content[0].contributor.email").value("alice@test.com"))
                .andExpect(jsonPath("$.content[0].totalCommits").value(25))
                .andExpect(jsonPath("$.content[0].totalChanges").value(120))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/contributors/{id} should return contributor details")
    void getContributorById() throws Exception {
        Instant now = Instant.parse("2026-09-01T12:00:00Z");
        ContributorResponse contributor = new ContributorResponse(10L, "alice@test.com", "alice", "Alice", "https://avatar.url", 12345L, now, now);

        when(contributorService.getContributorById(10L)).thenReturn(contributor);

        mockMvc.perform(get("/api/v1/contributors/10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.email").value("alice@test.com"))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.avatarUrl").value("https://avatar.url"))
                .andExpect(jsonPath("$.githubId").value(12345));
    }

    @Test
    @DisplayName("GET /api/v1/repositories/{id}/contributors/{contributorId} should return repository contributor attribution")
    void getRepositoryContributor() throws Exception {
        Instant now = Instant.parse("2026-09-01T12:00:00Z");
        ContributorResponse contributor = new ContributorResponse(10L, "alice@test.com", "alice", "Alice", null, null, now, now);
        RepositoryContributorResponse rc = new RepositoryContributorResponse(100L, 1L, contributor, 25, 100, 20, 120, now, now, now, now);

        when(contributorService.getRepositoryContributor(1L, 10L)).thenReturn(rc);

        mockMvc.perform(get("/api/v1/repositories/1/contributors/10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.repositoryId").value(1))
                .andExpect(jsonPath("$.contributor.id").value(10))
                .andExpect(jsonPath("$.totalCommits").value(25));
    }

    @Test
    @DisplayName("GET /api/v1/contributors/{id} should return 404 when contributor does not exist")
    void getContributorNotFound() throws Exception {
        when(contributorService.getContributorById(999L))
                .thenThrow(new ResourceNotFoundException("Contributor", "id", 999L));

        mockMvc.perform(get("/api/v1/contributors/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Contributor not found with id: '999'"));
    }
}
