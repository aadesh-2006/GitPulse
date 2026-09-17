package com.gitpulse.domain.contributorfile;

import com.gitpulse.common.exception.AppException;
import com.gitpulse.common.exception.GlobalExceptionHandler;
import com.gitpulse.common.exception.ResourceNotFoundException;
import com.gitpulse.domain.contributorfile.dto.RepositoryContributorFileResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RepositoryContributorFileController.class)
@Import(GlobalExceptionHandler.class)
class RepositoryContributorFileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RepositoryContributorFileQueryService queryService;

    private RepositoryContributorFileResponse sampleResponse(Long id, Long repoId, Long contribId, String email, String filePath, long churn) {
        return new RepositoryContributorFileResponse(
                id,
                repoId,
                new RepositoryContributorFileResponse.ContributorSummary(contribId, email, "user" + contribId, "Name " + contribId, "https://avatar.com/" + contribId),
                filePath,
                5,
                churn - 10,
                10,
                churn,
                Instant.parse("2026-09-01T10:00:00Z"),
                Instant.parse("2026-09-05T12:00:00Z"),
                Instant.parse("2026-09-01T10:00:00Z"),
                Instant.parse("2026-09-05T12:00:00Z")
        );
    }

    @Test
    @DisplayName("1. GET contributor-files returns paginated response with default totalChurn DESC")
    void getContributorFiles_Default() throws Exception {
        RepositoryContributorFileResponse r1 = sampleResponse(1L, 10L, 20L, "alice@corp.com", "src/App.java", 100);
        Page<RepositoryContributorFileResponse> page = new PageImpl<>(List.of(r1), PageRequest.of(0, 20), 1);

        when(queryService.getRepositoryContributorFiles(eq(10L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/repositories/10/contributor-files")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.content[0].repositoryId", is(10)))
                .andExpect(jsonPath("$.content[0].contributor.id", is(20)))
                .andExpect(jsonPath("$.content[0].contributor.email", is("alice@corp.com")))
                .andExpect(jsonPath("$.content[0].filePath", is("src/App.java")))
                .andExpect(jsonPath("$.content[0].totalChurn", is(100)))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    @DisplayName("2. GET contributor-files rejects invalid sort with HTTP 400")
    void getContributorFiles_InvalidSort() throws Exception {
        when(queryService.getRepositoryContributorFiles(eq(10L), any(Pageable.class)))
                .thenThrow(new AppException("Invalid sort field: 'maliciousField'"));

        mockMvc.perform(get("/api/v1/repositories/10/contributor-files?sort=maliciousField,asc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Invalid sort field: 'maliciousField'")));
    }

    @Test
    @DisplayName("3. GET contributor-files returns 404 when repository does not exist")
    void getContributorFiles_NotFound() throws Exception {
        when(queryService.getRepositoryContributorFiles(eq(999L), any(Pageable.class)))
                .thenThrow(new ResourceNotFoundException("Repository not found with id: 999"));

        mockMvc.perform(get("/api/v1/repositories/999/contributor-files")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Repository not found with id: 999")));
    }

    @Test
    @DisplayName("4. GET contributor files by contributor returns paginated data")
    void getContributorFilesByContributor_Success() throws Exception {
        RepositoryContributorFileResponse r1 = sampleResponse(1L, 10L, 20L, "alice@corp.com", "src/App.java", 100);
        Page<RepositoryContributorFileResponse> page = new PageImpl<>(List.of(r1), PageRequest.of(0, 20), 1);

        when(queryService.getContributorFilesByContributor(eq(10L), eq(20L), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/repositories/10/contributors/20/files")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].contributor.id", is(20)))
                .andExpect(jsonPath("$.content[0].filePath", is("src/App.java")));

        verify(queryService).getContributorFilesByContributor(eq(10L), eq(20L), any(Pageable.class));
    }

    @Test
    @DisplayName("5. GET contributor files by contributor returns 404 when contributor not found")
    void getContributorFilesByContributor_ContributorNotFound() throws Exception {
        when(queryService.getContributorFilesByContributor(eq(10L), eq(999L), any(Pageable.class)))
                .thenThrow(new ResourceNotFoundException("Contributor not found with id: 999"));

        mockMvc.perform(get("/api/v1/repositories/10/contributors/999/files")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Contributor not found with id: 999")));
    }

    @Test
    @DisplayName("6. GET file contributors returns paginated data")
    void getContributorFilesByFilePath_Success() throws Exception {
        RepositoryContributorFileResponse r1 = sampleResponse(1L, 10L, 20L, "alice@corp.com", "src/App.java", 100);
        Page<RepositoryContributorFileResponse> page = new PageImpl<>(List.of(r1), PageRequest.of(0, 20), 1);

        when(queryService.getContributorFilesByFilePath(eq(10L), eq("src/App.java"), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/repositories/10/files/contributors?filePath=src/App.java")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].filePath", is("src/App.java")));
    }

    @Test
    @DisplayName("7. GET file contributors returns 400 when filePath is missing or blank")
    void getContributorFilesByFilePath_BlankThrows400() throws Exception {
        when(queryService.getContributorFilesByFilePath(eq(10L), eq(""), any(Pageable.class)))
                .thenThrow(new AppException("filePath query parameter must not be blank"));

        mockMvc.perform(get("/api/v1/repositories/10/files/contributors?filePath=")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("filePath query parameter must not be blank")));
    }

    @Test
    @DisplayName("8. GET single contributor file returns exact record with nested file path")
    void getContributorFile_Success() throws Exception {
        RepositoryContributorFileResponse r1 = sampleResponse(1L, 10L, 20L, "alice@corp.com", "src/main/java/App.java", 100);

        when(queryService.getContributorFile(eq(10L), eq(20L), eq("/src/main/java/App.java")))
                .thenReturn(r1);

        mockMvc.perform(get("/api/v1/repositories/10/contributors/20/files/src/main/java/App.java")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.repositoryId", is(10)))
                .andExpect(jsonPath("$.contributor.id", is(20)))
                .andExpect(jsonPath("$.filePath", is("src/main/java/App.java")));
    }

    @Test
    @DisplayName("9. GET single contributor file returns 404 when relationship does not exist")
    void getContributorFile_NotFound() throws Exception {
        when(queryService.getContributorFile(eq(10L), eq(20L), eq("/unknown/File.java")))
                .thenThrow(new ResourceNotFoundException("Contributor-file relationship not found"));

        mockMvc.perform(get("/api/v1/repositories/10/contributors/20/files/unknown/File.java")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("10. GET contributor-files binds custom page, size, and sort into Pageable")
    void getContributorFiles_PageableBinding() throws Exception {
        when(queryService.getRepositoryContributorFiles(eq(10L), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/repositories/10/contributor-files?sort=totalRevisions,asc&page=1&size=5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        org.mockito.ArgumentCaptor<Pageable> captor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(queryService).getRepositoryContributorFiles(eq(10L), captor.capture());

        Pageable captured = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(captured.getPageNumber()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(captured.getPageSize()).isEqualTo(5);
        org.assertj.core.api.Assertions.assertThat(captured.getSort().getOrderFor("totalRevisions")).isNotNull();
        org.assertj.core.api.Assertions.assertThat(captured.getSort().getOrderFor("totalRevisions").getDirection())
                .isEqualTo(org.springframework.data.domain.Sort.Direction.ASC);
    }

    @Test
    @DisplayName("11. GET file-ownership returns paginated data with default sort")
    void getFileOwnership_Default() throws Exception {
        com.gitpulse.domain.contributorfile.dto.RepositoryFileOwnershipResponse response =
                new com.gitpulse.domain.contributorfile.dto.RepositoryFileOwnershipResponse(
                        10L,
                        "src/App.java",
                        3,
                        20,
                        new com.gitpulse.domain.contributorfile.dto.RepositoryFileOwnershipResponse.ContributorSummary(1L, "alice@corp.com", "alice", "Alice", "https://avatar.com/1"),
                        0.5
                );
        Page<com.gitpulse.domain.contributorfile.dto.RepositoryFileOwnershipResponse> page =
                new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1);

        when(queryService.getRepositoryFileOwnership(eq(10L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/repositories/10/file-ownership")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].repositoryId", is(10)))
                .andExpect(jsonPath("$.content[0].filePath", is("src/App.java")))
                .andExpect(jsonPath("$.content[0].contributorCount", is(3)))
                .andExpect(jsonPath("$.content[0].totalRevisionsAcrossContributors", is(20)))
                .andExpect(jsonPath("$.content[0].topContributor.id", is(1)))
                .andExpect(jsonPath("$.content[0].topContributorRevisionShare", is(0.5)));
    }

    @Test
    @DisplayName("12. GET file-ownership rejects invalid sort with HTTP 400")
    void getFileOwnership_InvalidSort() throws Exception {
        when(queryService.getRepositoryFileOwnership(eq(10L), any(Pageable.class)))
                .thenThrow(new AppException("Invalid sort field: 'unsupported'"));

        mockMvc.perform(get("/api/v1/repositories/10/file-ownership?sort=unsupported,asc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Invalid sort field: 'unsupported'")));
    }

    @Test
    @DisplayName("13. GET file-ownership binds custom page, size, and sort into Pageable")
    void getFileOwnership_PageableBinding() throws Exception {
        when(queryService.getRepositoryFileOwnership(eq(10L), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/repositories/10/file-ownership?sort=totalRevisionsAcrossContributors,asc&page=2&size=10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        org.mockito.ArgumentCaptor<Pageable> captor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(queryService).getRepositoryFileOwnership(eq(10L), captor.capture());

        Pageable captured = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(captured.getPageNumber()).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(captured.getPageSize()).isEqualTo(10);
        org.assertj.core.api.Assertions.assertThat(captured.getSort().getOrderFor("totalRevisionsAcrossContributors")).isNotNull();
        org.assertj.core.api.Assertions.assertThat(captured.getSort().getOrderFor("totalRevisionsAcrossContributors").getDirection())
                .isEqualTo(org.springframework.data.domain.Sort.Direction.ASC);
    }
}
