package com.gitpulse.domain.commit;

import com.gitpulse.common.exception.AppException;
import com.gitpulse.common.exception.GlobalExceptionHandler;
import com.gitpulse.common.exception.ResourceNotFoundException;
import com.gitpulse.domain.commit.dto.CommitDetailResponse;
import com.gitpulse.domain.commit.dto.CommitFileChangeResponse;
import com.gitpulse.domain.commit.dto.CommitResponse;
import com.gitpulse.domain.filechange.FileChangeStatus;
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

@WebMvcTest(CommitController.class)
@Import(GlobalExceptionHandler.class)
class CommitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommitQueryService commitQueryService;

    private CommitResponse sampleCommitResponse(Long id, String sha, CommitClassification classification) {
        return new CommitResponse(
                id,
                1L,
                sha,
                "feat: some message",
                "Alice",
                "alice@example.com",
                "alice",
                Instant.parse("2026-09-01T10:00:00Z"),
                10,
                2,
                12,
                "https://github.com/owner/repo/commit/" + sha,
                classification,
                Instant.parse("2026-09-01T10:00:00Z")
        );
    }

    @Test
    @DisplayName("GET repository commits returns paginated commits with default sorting")
    void getRepositoryCommits_Default() throws Exception {
        CommitResponse commit = sampleCommitResponse(100L, "sha12345", CommitClassification.FEATURE);
        Page<CommitResponse> page = new PageImpl<>(List.of(commit), PageRequest.of(0, 20), 1);

        when(commitQueryService.getRepositoryCommits(eq(1L), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/repositories/1/commits")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(100)))
                .andExpect(jsonPath("$.content[0].githubCommitSha", is("sha12345")))
                .andExpect(jsonPath("$.content[0].classification", is("FEATURE")))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    @DisplayName("GET repository commits with classification filter")
    void getRepositoryCommits_ClassificationFilter() throws Exception {
        when(commitQueryService.getRepositoryCommits(eq(1L), eq(CommitClassification.BUG_FIX), any(), any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/repositories/1/commits?classification=BUG_FIX")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(commitQueryService).getRepositoryCommits(eq(1L), eq(CommitClassification.BUG_FIX), any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("GET repository commits with authorEmail filter")
    void getRepositoryCommits_AuthorEmailFilter() throws Exception {
        when(commitQueryService.getRepositoryCommits(eq(1L), any(), eq("bob@example.com"), any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/repositories/1/commits?authorEmail=bob@example.com")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(commitQueryService).getRepositoryCommits(eq(1L), any(), eq("bob@example.com"), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("GET repository commits with date range filter (from/to)")
    void getRepositoryCommits_DateRangeFilter() throws Exception {
        Instant from = Instant.parse("2026-09-01T00:00:00Z");
        Instant to = Instant.parse("2026-09-02T00:00:00Z");

        when(commitQueryService.getRepositoryCommits(eq(1L), any(), any(), eq(from), eq(to), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/repositories/1/commits?from=2026-09-01T00:00:00Z&to=2026-09-02T00:00:00Z")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(commitQueryService).getRepositoryCommits(eq(1L), any(), any(), eq(from), eq(to), any(Pageable.class));
    }

    @Test
    @DisplayName("GET repository commits with combined filters, pagination, and sorting")
    void getRepositoryCommits_CombinedFilters() throws Exception {
        when(commitQueryService.getRepositoryCommits(eq(1L), eq(CommitClassification.REFACTOR), eq("alice@example.com"), any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/repositories/1/commits?classification=REFACTOR&authorEmail=alice@example.com&page=1&size=10&sort=totalChanges,desc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(commitQueryService).getRepositoryCommits(eq(1L), eq(CommitClassification.REFACTOR), eq("alice@example.com"), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("GET repository commits returns 404 when repository does not exist")
    void getRepositoryCommits_RepoNotFound() throws Exception {
        when(commitQueryService.getRepositoryCommits(eq(999L), any(), any(), any(), any(), any(Pageable.class)))
                .thenThrow(new ResourceNotFoundException("Repository not found with id: 999"));

        mockMvc.perform(get("/api/v1/repositories/999/commits"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Repository not found with id: 999")));
    }

    @Test
    @DisplayName("GET repository commits returns 400 on invalid sort field")
    void getRepositoryCommits_InvalidSortField() throws Exception {
        when(commitQueryService.getRepositoryCommits(eq(1L), any(), any(), any(), any(), any(Pageable.class)))
                .thenThrow(new AppException("Invalid sort field: 'injectedField'"));

        mockMvc.perform(get("/api/v1/repositories/1/commits?sort=injectedField,desc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Invalid sort field: 'injectedField'")));
    }

    @Test
    @DisplayName("GET repository commits returns 400 on invalid sort direction")
    void getRepositoryCommits_InvalidSortDirection() throws Exception {
        when(commitQueryService.getRepositoryCommits(eq(1L), any(), any(), any(), any(), any(Pageable.class)))
                .thenThrow(new IllegalArgumentException("Invalid sort direction"));

        mockMvc.perform(get("/api/v1/repositories/1/commits?sort=committedAt,invalidDirection"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Invalid sort direction")));
    }

    @Test
    @DisplayName("GET repository commits supports explicit valid sorting")
    void getRepositoryCommits_ExplicitSorting() throws Exception {
        when(commitQueryService.getRepositoryCommits(eq(1L), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/repositories/1/commits?sort=totalChanges,asc"))
                .andExpect(status().isOk());

        verify(commitQueryService).getRepositoryCommits(eq(1L), any(), any(), any(), any(), any(Pageable.class));
    }


    @Test
    @DisplayName("GET commit detail returns commit metadata and associated file changes")
    void getCommitDetail_Success() throws Exception {
        CommitFileChangeResponse fc1 = new CommitFileChangeResponse(
                50L, 10L, "src/App.java", FileChangeStatus.MODIFIED, 5, 1, 6, "blob1", "raw1", Instant.parse("2026-09-01T10:00:00Z")
        );
        CommitDetailResponse detail = new CommitDetailResponse(
                10L,
                1L,
                "sha999",
                "feat: add feature",
                "Alice",
                "alice@example.com",
                "alice",
                Instant.parse("2026-09-01T10:00:00Z"),
                5,
                1,
                6,
                "https://github.com/owner/repo/commit/sha999",
                CommitClassification.FEATURE,
                Instant.parse("2026-09-01T10:00:00Z"),
                List.of(fc1)
        );

        when(commitQueryService.getCommitDetail(1L, 10L)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/repositories/1/commits/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.githubCommitSha", is("sha999")))
                .andExpect(jsonPath("$.classification", is("FEATURE")))
                .andExpect(jsonPath("$.fileChanges", hasSize(1)))
                .andExpect(jsonPath("$.fileChanges[0].filePath", is("src/App.java")))
                .andExpect(jsonPath("$.fileChanges[0].status", is("MODIFIED")));
    }

    @Test
    @DisplayName("GET commit detail returns 404 when commit is not found in repository")
    void getCommitDetail_NotFound() throws Exception {
        when(commitQueryService.getCommitDetail(1L, 999L))
                .thenThrow(new ResourceNotFoundException("Commit not found with id: 999 for repository id: 1"));

        mockMvc.perform(get("/api/v1/repositories/1/commits/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Commit not found with id: 999 for repository id: 1")));
    }
}
