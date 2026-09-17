package com.gitpulse.domain.file;

import com.gitpulse.common.exception.AppException;
import com.gitpulse.common.exception.GlobalExceptionHandler;
import com.gitpulse.common.exception.ResourceNotFoundException;
import com.gitpulse.domain.file.dto.PrimaryContributorSummaryResponse;
import com.gitpulse.domain.file.dto.RepositoryFileResponse;
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
import org.springframework.data.domain.Sort;
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

@WebMvcTest(RepositoryFileController.class)
@Import(GlobalExceptionHandler.class)
class RepositoryFileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RepositoryFileQueryService repositoryFileQueryService;

    private RepositoryFileResponse sampleFile(Long id, String path, String name, String ext, String dir, int churn, boolean deleted) {
        return new RepositoryFileResponse(
                id,
                1L,
                path,
                name,
                ext,
                dir,
                5,
                churn - 10,
                10,
                churn,
                deleted,
                Instant.parse("2026-09-01T10:00:00Z"),
                Instant.parse("2026-09-05T12:00:00Z"),
                new PrimaryContributorSummaryResponse(10L, "alice@corp.com", "alice", "Alice", "https://avatar.com/1"),
                Instant.parse("2026-09-01T10:00:00Z"),
                Instant.parse("2026-09-05T12:00:00Z")
        );
    }

    @Test
    @DisplayName("A. GET repository files returns paginated data with default sort")
    void getRepositoryFiles_Default() throws Exception {
        RepositoryFileResponse file1 = sampleFile(100L, "src/App.java", "App.java", "java", "src", 150, false);
        Page<RepositoryFileResponse> page = new PageImpl<>(List.of(file1), PageRequest.of(0, 20), 1);

        when(repositoryFileQueryService.getRepositoryFiles(eq(1L), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/repositories/1/files")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(100)))
                .andExpect(jsonPath("$.content[0].filePath", is("src/App.java")))
                .andExpect(jsonPath("$.content[0].totalChurn", is(150)))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    @DisplayName("B. GET repository files supports explicit sorting")
    void getRepositoryFiles_ExplicitSorting() throws Exception {
        when(repositoryFileQueryService.getRepositoryFiles(eq(1L), any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/repositories/1/files?sort=totalRevisions,desc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(repositoryFileQueryService).getRepositoryFiles(eq(1L), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("C. GET repository files with extension filter")
    void getRepositoryFiles_ExtensionFilter() throws Exception {
        when(repositoryFileQueryService.getRepositoryFiles(eq(1L), eq("java"), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/repositories/1/files?extension=java")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(repositoryFileQueryService).getRepositoryFiles(eq(1L), eq("java"), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("D. GET repository files with isDeleted filter")
    void getRepositoryFiles_DeletedFilter() throws Exception {
        when(repositoryFileQueryService.getRepositoryFiles(eq(1L), any(), eq(true), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/repositories/1/files?isDeleted=true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(repositoryFileQueryService).getRepositoryFiles(eq(1L), any(), eq(true), any(Pageable.class));
    }

    @Test
    @DisplayName("E. Combined filters and pagination")
    void getRepositoryFiles_CombinedFilters() throws Exception {
        when(repositoryFileQueryService.getRepositoryFiles(eq(1L), eq("md"), eq(false), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/repositories/1/files?extension=md&isDeleted=false&page=1&size=10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(repositoryFileQueryService).getRepositoryFiles(eq(1L), eq("md"), eq(false), any(Pageable.class));
    }

    @Test
    @DisplayName("F. Repository not found returns 404")
    void getRepositoryFiles_RepoNotFound() throws Exception {
        when(repositoryFileQueryService.getRepositoryFiles(eq(999L), any(), any(), any(Pageable.class)))
                .thenThrow(new ResourceNotFoundException("Repository not found with id: 999"));

        mockMvc.perform(get("/api/v1/repositories/999/files"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Repository not found with id: 999")));
    }

    @Test
    @DisplayName("G & I. File detail endpoint resolves nested path containing multiple '/'")
    void getRepositoryFileByPath_Success() throws Exception {
        RepositoryFileResponse response = sampleFile(200L, "src/main/java/com/gitpulse/App.java", "App.java", "java", "src/main/java/com/gitpulse", 250, false);
        when(repositoryFileQueryService.getRepositoryFileByPath(1L, "src/main/java/com/gitpulse/App.java"))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/repositories/1/files/src/main/java/com/gitpulse/App.java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(200)))
                .andExpect(jsonPath("$.filePath", is("src/main/java/com/gitpulse/App.java")))
                .andExpect(jsonPath("$.fileName", is("App.java")))
                .andExpect(jsonPath("$.extension", is("java")))
                .andExpect(jsonPath("$.directoryPath", is("src/main/java/com/gitpulse")))
                .andExpect(jsonPath("$.totalChurn", is(250)))
                .andExpect(jsonPath("$.primaryContributor.email", is("alice@corp.com")));
    }

    @Test
    @DisplayName("H. File detail not found returns 404")
    void getRepositoryFileByPath_NotFound() throws Exception {
        when(repositoryFileQueryService.getRepositoryFileByPath(1L, "nonexistent.txt"))
                .thenThrow(new ResourceNotFoundException("File not found with path 'nonexistent.txt' in repository id: 1"));

        mockMvc.perform(get("/api/v1/repositories/1/files/nonexistent.txt"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("File not found with path 'nonexistent.txt' in repository id: 1")));
    }

    @Test
    @DisplayName("J. Hotspots query returns paginated files ranked by churn")
    void getRepositoryHotspots_Success() throws Exception {
        RepositoryFileResponse hot1 = sampleFile(1L, "Hot.java", "Hot.java", "java", null, 500, false);
        RepositoryFileResponse hot2 = sampleFile(2L, "Warm.java", "Warm.java", "java", null, 200, false);
        Page<RepositoryFileResponse> page = new PageImpl<>(List.of(hot1, hot2), PageRequest.of(0, 10, Sort.by("totalChurn").descending()), 2);

        when(repositoryFileQueryService.getRepositoryHotspots(eq(1L), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/repositories/1/files/hotspots?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].filePath", is("Hot.java")))
                .andExpect(jsonPath("$.content[0].totalChurn", is(500)))
                .andExpect(jsonPath("$.content[1].filePath", is("Warm.java")))
                .andExpect(jsonPath("$.content[1].totalChurn", is(200)));
    }

    @Test
    @DisplayName("K. Invalid sort field returns 400 Bad Request")
    void invalidSortField_Returns400() throws Exception {
        when(repositoryFileQueryService.getRepositoryFiles(eq(1L), any(), any(), any(Pageable.class)))
                .thenThrow(new AppException("Invalid sort field: 'injectedField'"));

        mockMvc.perform(get("/api/v1/repositories/1/files?sort=injectedField,desc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Invalid sort field: 'injectedField'")));
    }

    @Test
    @DisplayName("L. Invalid sort direction returns 400 Bad Request")
    void invalidSortDirection_Returns400() throws Exception {
        when(repositoryFileQueryService.getRepositoryFiles(eq(1L), any(), any(), any(Pageable.class)))
                .thenThrow(new IllegalArgumentException("Invalid sort direction"));

        mockMvc.perform(get("/api/v1/repositories/1/files?sort=totalChurn,invalidDirection"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Invalid sort direction")));
    }
}