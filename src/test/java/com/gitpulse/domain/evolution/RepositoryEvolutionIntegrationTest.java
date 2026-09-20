package com.gitpulse.domain.evolution;

import com.gitpulse.domain.commit.Commit;
import com.gitpulse.domain.commit.CommitClassification;
import com.gitpulse.domain.commit.CommitJpaRepository;
import com.gitpulse.domain.filechange.FileChange;
import com.gitpulse.domain.filechange.FileChangeJpaRepository;
import com.gitpulse.domain.filechange.FileChangeStatus;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RepositoryEvolutionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RepositoryJpaRepository repositoryJpaRepository;

    @Autowired
    private CommitJpaRepository commitJpaRepository;

    @Autowired
    private FileChangeJpaRepository fileChangeJpaRepository;

    private Repository repo1;
    private Repository repo2;

    @BeforeEach
    void setUp() {
        repo1 = repositoryJpaRepository.save(new Repository("spring-projects", "spring-boot", "Spring Boot", "main"));
        repo2 = repositoryJpaRepository.save(new Repository("quarkusio", "quarkus", "Quarkus", "main"));
    }

    @Test
    @DisplayName("GET /api/v1/repositories/{id}/evolution - Range with multiple consecutive inactive months returns SQL zero-filled buckets")
    void getRepositoryEvolution_MultipleInactiveMonths_ReturnsConsecutiveZeroBuckets() throws Exception {
        // Activity only in January 2026 and June 2026; Feb, Mar, Apr, May are inactive
        Commit janCommit = createCommit(repo1, "sha_jan_multi", "feat: start project", "Alice", "alice@example.com", "alice",
                Instant.parse("2026-01-10T10:00:00Z"), 20, 5, 25, CommitClassification.FEATURE);
        createFileChange(janCommit, "src/Init.java", 20, 5, 25);

        Commit junCommit = createCommit(repo1, "sha_jun_multi", "feat: resume project", "Alice", "alice@example.com", "alice",
                Instant.parse("2026-06-15T12:00:00Z"), 50, 10, 60, CommitClassification.FEATURE);
        createFileChange(junCommit, "src/Feature.java", 50, 10, 60);

        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution", repo1.getId())
                        .param("from", "2026-01-01T00:00:00Z")
                        .param("to", "2026-07-01T00:00:00Z")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buckets", hasSize(6)))
                // Month 1: Jan (active)
                .andExpect(jsonPath("$.buckets[0].month", is("2026-01-01T00:00:00Z")))
                .andExpect(jsonPath("$.buckets[0].totalCommits", is(1)))
                .andExpect(jsonPath("$.buckets[0].totalChurn", is(25)))
                // Month 2: Feb (zero-fill)
                .andExpect(jsonPath("$.buckets[1].month", is("2026-02-01T00:00:00Z")))
                .andExpect(jsonPath("$.buckets[1].totalCommits", is(0)))
                .andExpect(jsonPath("$.buckets[1].totalChurn", is(0)))
                // Month 3: Mar (zero-fill)
                .andExpect(jsonPath("$.buckets[2].month", is("2026-03-01T00:00:00Z")))
                .andExpect(jsonPath("$.buckets[2].totalCommits", is(0)))
                .andExpect(jsonPath("$.buckets[2].totalChurn", is(0)))
                // Month 4: Apr (zero-fill)
                .andExpect(jsonPath("$.buckets[3].month", is("2026-04-01T00:00:00Z")))
                .andExpect(jsonPath("$.buckets[3].totalCommits", is(0)))
                .andExpect(jsonPath("$.buckets[3].totalChurn", is(0)))
                // Month 5: May (zero-fill)
                .andExpect(jsonPath("$.buckets[4].month", is("2026-05-01T00:00:00Z")))
                .andExpect(jsonPath("$.buckets[4].totalCommits", is(0)))
                .andExpect(jsonPath("$.buckets[4].totalChurn", is(0)))
                // Month 6: Jun (active)
                .andExpect(jsonPath("$.buckets[5].month", is("2026-06-01T00:00:00Z")))
                .andExpect(jsonPath("$.buckets[5].totalCommits", is(1)))
                .andExpect(jsonPath("$.buckets[5].totalChurn", is(60)));
    }

    @Test
    @DisplayName("GET /api/v1/repositories/{id}/evolution - Comprehensive monthly aggregation with classifications, file changes, and zero-filled inactive months")
    void getRepositoryEvolution_FullActivityScenario() throws Exception {
        // January 2026 Commits (repo1)
        Commit janCommit1 = createCommit(repo1, "sha_jan_1", "feat: add oauth2 login", "Alice", "alice@example.com", "alice",
                Instant.parse("2026-01-05T10:00:00Z"), 50, 10, 60, CommitClassification.FEATURE);
        createFileChange(janCommit1, "src/main/Auth.java", 30, 5, 35);
        createFileChange(janCommit1, "src/main/SecurityConfig.java", 20, 5, 25);

        Commit janCommit2 = createCommit(repo1, "sha_jan_2", "fix: null pointer in token validation", "Alice", "  ALICE@example.com  ", "alice",
                Instant.parse("2026-01-15T14:30:00Z"), 5, 2, 7, CommitClassification.BUG_FIX);
        createFileChange(janCommit2, "src/main/Auth.java", 5, 2, 7); // duplicate file path in same month

        Commit janCommit3 = createCommit(repo1, "sha_jan_3", "refactor: simplify token parsing", "Bob", "bob@example.com", "bob",
                Instant.parse("2026-01-28T09:00:00Z"), 20, 15, 35, CommitClassification.REFACTOR);
        createFileChange(janCommit3, "src/main/TokenParser.java", 20, 15, 35);

        // February 2026: NO COMMITS (Should produce a zero-filled bucket)

        // March 2026 Commits (repo1)
        Commit marCommit1 = createCommit(repo1, "sha_mar_1", "docs: update auth setup guide", "Charlie", "charlie@example.com", "charlie",
                Instant.parse("2026-03-02T11:00:00Z"), 40, 5, 45, CommitClassification.DOCUMENTATION);
        createFileChange(marCommit1, "README.md", 40, 5, 45);

        Commit marCommit2 = createCommit(repo1, "sha_mar_2", "test: add integration test", "Charlie", "charlie@example.com", "charlie",
                Instant.parse("2026-03-10T16:00:00Z"), 80, 0, 80, CommitClassification.TEST);
        createFileChange(marCommit2, "src/test/AuthIntegrationTest.java", 80, 0, 80);

        Commit marCommit3 = createCommit(repo1, "sha_mar_3", "build: bump gradle version", "DevOps", "devops@example.com", "devops",
                Instant.parse("2026-03-15T12:00:00Z"), 10, 10, 20, CommitClassification.BUILD);
        createFileChange(marCommit3, "build.gradle", 10, 10, 20);

        Commit marCommit4 = createCommit(repo1, "sha_mar_4", "config: update application yaml", "DevOps", "devops@example.com", "devops",
                Instant.parse("2026-03-20T10:00:00Z"), 8, 2, 10, CommitClassification.CONFIGURATION);
        createFileChange(marCommit4, "application.yml", 8, 2, 10);

        Commit marCommit5 = createCommit(repo1, "sha_mar_5", "deps: upgrade jackson to 2.15", "Dependabot", "bot@example.com", "dependabot",
                Instant.parse("2026-03-25T08:00:00Z"), 2, 2, 4, CommitClassification.DEPENDENCY);
        createFileChange(marCommit5, "build.gradle", 2, 2, 4);

        Commit marCommit6 = createCommit(repo1, "sha_mar_6", "chore: minor cleanup", "Bob", "bob@example.com", "bob",
                Instant.parse("2026-03-28T18:00:00Z"), 5, 5, 10, CommitClassification.OTHER);
        createFileChange(marCommit6, "src/main/TokenParser.java", 5, 5, 10);

        // Commit with null classification in March
        createCommit(repo1, "sha_mar_unclassified", "legacy commit without category", "Bob", "bob@example.com", "bob",
                Instant.parse("2026-03-29T19:00:00Z"), 15, 5, 20, null);

        // Repo 2 Commit in January (should be excluded due to repository isolation)
        Commit repo2Commit = createCommit(repo2, "sha_repo2_jan", "feat: quarkus reactive route", "Alice", "alice@example.com", "alice",
                Instant.parse("2026-01-10T10:00:00Z"), 100, 50, 150, CommitClassification.FEATURE);
        createFileChange(repo2Commit, "src/main/Route.java", 100, 50, 150);

        // Commit outside boundary (before from: 2025-12-31, and exactly at to: 2026-04-01)
        createCommit(repo1, "sha_dec_old", "old commit", "Alice", "alice@example.com", "alice",
                Instant.parse("2025-12-31T23:59:59Z"), 10, 10, 20, CommitClassification.FEATURE);
        createCommit(repo1, "sha_apr_excluded", "apr commit on boundary", "Alice", "alice@example.com", "alice",
                Instant.parse("2026-04-01T00:00:00Z"), 10, 10, 20, CommitClassification.FEATURE);

        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution", repo1.getId())
                        .param("from", "2026-01-01T00:00:00Z")
                        .param("to", "2026-04-01T00:00:00Z")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositoryId", is(repo1.getId().intValue())))
                .andExpect(jsonPath("$.from", is("2026-01-01T00:00:00Z")))
                .andExpect(jsonPath("$.to", is("2026-04-01T00:00:00Z")))
                .andExpect(jsonPath("$.buckets", hasSize(3)))
                // Month 1: January 2026
                .andExpect(jsonPath("$.buckets[0].month", is("2026-01-01T00:00:00Z")))
                .andExpect(jsonPath("$.buckets[0].totalCommits", is(3)))
                .andExpect(jsonPath("$.buckets[0].totalAdditions", is(75))) // 50 + 5 + 20
                .andExpect(jsonPath("$.buckets[0].totalDeletions", is(27))) // 10 + 2 + 15
                .andExpect(jsonPath("$.buckets[0].totalChurn", is(102)))    // 60 + 7 + 35
                .andExpect(jsonPath("$.buckets[0].activeContributors", is(2))) // Alice (normalized) and Bob
                .andExpect(jsonPath("$.buckets[0].filesChanged", is(3)))      // Auth.java, SecurityConfig.java, TokenParser.java
                .andExpect(jsonPath("$.buckets[0].featureCommits", is(1)))
                .andExpect(jsonPath("$.buckets[0].bugFixCommits", is(1)))
                .andExpect(jsonPath("$.buckets[0].refactorCommits", is(1)))
                .andExpect(jsonPath("$.buckets[0].documentationCommits", is(0)))
                .andExpect(jsonPath("$.buckets[0].testCommits", is(0)))
                .andExpect(jsonPath("$.buckets[0].buildCommits", is(0)))
                .andExpect(jsonPath("$.buckets[0].configurationCommits", is(0)))
                .andExpect(jsonPath("$.buckets[0].dependencyCommits", is(0)))
                .andExpect(jsonPath("$.buckets[0].otherCommits", is(0)))
                // Month 2: February 2026 (Zero-filled inactive month)
                .andExpect(jsonPath("$.buckets[1].month", is("2026-02-01T00:00:00Z")))
                .andExpect(jsonPath("$.buckets[1].totalCommits", is(0)))
                .andExpect(jsonPath("$.buckets[1].totalAdditions", is(0)))
                .andExpect(jsonPath("$.buckets[1].totalDeletions", is(0)))
                .andExpect(jsonPath("$.buckets[1].totalChurn", is(0)))
                .andExpect(jsonPath("$.buckets[1].activeContributors", is(0)))
                .andExpect(jsonPath("$.buckets[1].filesChanged", is(0)))
                .andExpect(jsonPath("$.buckets[1].featureCommits", is(0)))
                .andExpect(jsonPath("$.buckets[1].bugFixCommits", is(0)))
                .andExpect(jsonPath("$.buckets[1].refactorCommits", is(0)))
                .andExpect(jsonPath("$.buckets[1].documentationCommits", is(0)))
                .andExpect(jsonPath("$.buckets[1].testCommits", is(0)))
                .andExpect(jsonPath("$.buckets[1].buildCommits", is(0)))
                .andExpect(jsonPath("$.buckets[1].configurationCommits", is(0)))
                .andExpect(jsonPath("$.buckets[1].dependencyCommits", is(0)))
                .andExpect(jsonPath("$.buckets[1].otherCommits", is(0)))
                // Month 3: March 2026 (7 commits: 6 categorized + 1 unclassified)
                .andExpect(jsonPath("$.buckets[2].month", is("2026-03-01T00:00:00Z")))
                .andExpect(jsonPath("$.buckets[2].totalCommits", is(7)))
                .andExpect(jsonPath("$.buckets[2].totalAdditions", is(160))) // 40+80+10+8+2+5+15
                .andExpect(jsonPath("$.buckets[2].totalDeletions", is(29)))  // 5+0+10+2+2+5+5
                .andExpect(jsonPath("$.buckets[2].totalChurn", is(189)))     // 160 + 29
                .andExpect(jsonPath("$.buckets[2].activeContributors", is(4))) // Charlie, DevOps, Dependabot, Bob
                .andExpect(jsonPath("$.buckets[2].filesChanged", is(5)))       // README.md, AuthIntegrationTest.java, build.gradle, application.yml, TokenParser.java
                .andExpect(jsonPath("$.buckets[2].featureCommits", is(0)))
                .andExpect(jsonPath("$.buckets[2].bugFixCommits", is(0)))
                .andExpect(jsonPath("$.buckets[2].refactorCommits", is(0)))
                .andExpect(jsonPath("$.buckets[2].documentationCommits", is(1)))
                .andExpect(jsonPath("$.buckets[2].testCommits", is(1)))
                .andExpect(jsonPath("$.buckets[2].buildCommits", is(1)))
                .andExpect(jsonPath("$.buckets[2].configurationCommits", is(1)))
                .andExpect(jsonPath("$.buckets[2].dependencyCommits", is(1)))
                .andExpect(jsonPath("$.buckets[2].otherCommits", is(1))); // unclassified commit is NOT added to otherCommits
    }

    @Test
    @DisplayName("GET /api/v1/repositories/{id}/evolution - Exact boundary inclusiveness/exclusiveness")
    void getRepositoryEvolution_BoundarySemantics() throws Exception {
        // Commit exactly at 'from' timestamp (inclusive -> should be present in Jan)
        createCommit(repo1, "sha_exact_from", "msg", "A", "a@example.com", "a",
                Instant.parse("2026-01-01T00:00:00Z"), 10, 5, 15, CommitClassification.FEATURE);

        // Commit exactly at 'to' timestamp (exclusive -> should NOT be present)
        createCommit(repo1, "sha_exact_to", "msg", "A", "a@example.com", "a",
                Instant.parse("2026-02-01T00:00:00Z"), 20, 10, 30, CommitClassification.FEATURE);

        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution", repo1.getId())
                        .param("from", "2026-01-01T00:00:00Z")
                        .param("to", "2026-02-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buckets", hasSize(1)))
                .andExpect(jsonPath("$.buckets[0].month", is("2026-01-01T00:00:00Z")))
                .andExpect(jsonPath("$.buckets[0].totalCommits", is(1)))
                .andExpect(jsonPath("$.buckets[0].totalChurn", is(15)));
    }

    @Test
    @DisplayName("GET /api/v1/repositories/{id}/evolution - Exact boundary semantics: to exactly on month boundary excludes next month, to shortly after includes it")
    void getRepositoryEvolution_ExactSubSecondBoundarySemantics() throws Exception {
        // from = 2026-01-15 (middle of month), to = 2026-04-01T00:00:00Z (exact boundary) -> Jan, Feb, Mar (3 buckets, April excluded)
        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution", repo1.getId())
                        .param("from", "2026-01-15T12:30:00Z")
                        .param("to", "2026-04-01T00:00:00Z")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buckets", hasSize(3)))
                .andExpect(jsonPath("$.buckets[0].month", is("2026-01-01T00:00:00Z")))
                .andExpect(jsonPath("$.buckets[1].month", is("2026-02-01T00:00:00Z")))
                .andExpect(jsonPath("$.buckets[2].month", is("2026-03-01T00:00:00Z")));

        // to shortly after month boundary: 2026-04-01T00:00:00.000001Z -> Jan, Feb, Mar, Apr (4 buckets, April included)
        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution", repo1.getId())
                        .param("from", "2026-01-15T12:30:00Z")
                        .param("to", "2026-04-01T00:00:00.000001Z")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buckets", hasSize(4)))
                .andExpect(jsonPath("$.buckets[0].month", is("2026-01-01T00:00:00Z")))
                .andExpect(jsonPath("$.buckets[1].month", is("2026-02-01T00:00:00Z")))
                .andExpect(jsonPath("$.buckets[2].month", is("2026-03-01T00:00:00Z")))
                .andExpect(jsonPath("$.buckets[3].month", is("2026-04-01T00:00:00Z")));
    }

    @Test
    @DisplayName("GET /api/v1/repositories/{id}/evolution - Single commit with multiple file changes does not inflate commit-level metrics")
    void getRepositoryEvolution_CommitWithMultipleFileChanges_DoesNotMultiplyCommitMetrics() throws Exception {
        // Single commit with 3 file changes
        Commit commit = createCommit(repo1, "sha_multi_files", "feat: big refactor", "Dev", "dev@example.com", "dev",
                Instant.parse("2026-01-10T10:00:00Z"), 100, 50, 150, CommitClassification.FEATURE);
        createFileChange(commit, "src/A.java", 50, 20, 70);
        createFileChange(commit, "src/B.java", 30, 20, 50);
        createFileChange(commit, "src/C.java", 20, 10, 30);

        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution", repo1.getId())
                        .param("from", "2026-01-01T00:00:00Z")
                        .param("to", "2026-02-01T00:00:00Z")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buckets", hasSize(1)))
                .andExpect(jsonPath("$.buckets[0].totalCommits", is(1)))       // Exactly 1 commit, NOT 3
                .andExpect(jsonPath("$.buckets[0].totalAdditions", is(100)))    // Exactly 100, NOT 300
                .andExpect(jsonPath("$.buckets[0].totalDeletions", is(50)))     // Exactly 50, NOT 150
                .andExpect(jsonPath("$.buckets[0].totalChurn", is(150)))        // Exactly 150, NOT 450
                .andExpect(jsonPath("$.buckets[0].activeContributors", is(1)))  // Exactly 1 contributor
                .andExpect(jsonPath("$.buckets[0].filesChanged", is(3)))        // 3 distinct files
                .andExpect(jsonPath("$.buckets[0].featureCommits", is(1)));     // Exactly 1 feature commit
    }

    @Test
    @DisplayName("GET /api/v1/repositories/{id}/evolution - Empty repository returns zero-filled buckets for all requested months")
    void getRepositoryEvolution_EmptyRepository_ReturnsZeroBuckets() throws Exception {
        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution", repo1.getId())
                        .param("from", "2026-01-01T00:00:00Z")
                        .param("to", "2026-04-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buckets", hasSize(3)))
                .andExpect(jsonPath("$.buckets[0].month", is("2026-01-01T00:00:00Z")))
                .andExpect(jsonPath("$.buckets[0].totalCommits", is(0)))
                .andExpect(jsonPath("$.buckets[1].month", is("2026-02-01T00:00:00Z")))
                .andExpect(jsonPath("$.buckets[1].totalCommits", is(0)))
                .andExpect(jsonPath("$.buckets[2].month", is("2026-03-01T00:00:00Z")))
                .andExpect(jsonPath("$.buckets[2].totalCommits", is(0)));
    }

    @Test
    @DisplayName("GET /api/v1/repositories/{id}/evolution - Invalid range where from >= to returns 400 Bad Request")
    void getRepositoryEvolution_InvalidDateRange_Returns400() throws Exception {
        // from == to
        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution", repo1.getId())
                        .param("from", "2026-01-01T00:00:00Z")
                        .param("to", "2026-01-01T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("The 'from' timestamp must be strictly before the 'to' timestamp")));

        // from > to
        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution", repo1.getId())
                        .param("from", "2026-05-01T00:00:00Z")
                        .param("to", "2026-01-01T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("The 'from' timestamp must be strictly before the 'to' timestamp")));
    }

    @Test
    @DisplayName("GET /api/v1/repositories/{id}/evolution - Missing from or to parameter returns 400 Bad Request")
    void getRepositoryEvolution_MissingParameters_Returns400() throws Exception {
        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution", repo1.getId())
                        .param("from", "2026-01-01T00:00:00Z"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution", repo1.getId())
                        .param("to", "2026-01-01T00:00:00Z"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/repositories/{id}/evolution - Invalid timestamp format returns 400 Bad Request")
    void getRepositoryEvolution_InvalidTimestampFormat_Returns400() throws Exception {
        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution", repo1.getId())
                        .param("from", "invalid-timestamp")
                        .param("to", "2026-03-01T00:00:00Z"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution", repo1.getId())
                        .param("from", "2026-01-01T00:00:00Z")
                        .param("to", "not-a-valid-date"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/repositories/{id}/evolution - Non-existent repository returns 404 Not Found")
    void getRepositoryEvolution_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution", 99999L)
                        .param("from", "2026-01-01T00:00:00Z")
                        .param("to", "2026-03-01T00:00:00Z"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Repository not found with id: 99999")));
    }

    private Commit createCommit(Repository repo, String sha, String message, String authorName, String authorEmail,
                                String authorUsername, Instant committedAt, Integer additions, Integer deletions,
                                Integer totalChanges, CommitClassification classification) {
        Commit commit = new Commit(
                repo,
                sha,
                message,
                authorName,
                authorEmail,
                authorUsername,
                committedAt,
                additions,
                deletions,
                totalChanges,
                "https://github.com/" + repo.getFullName() + "/commit/" + sha,
                classification
        );
        return commitJpaRepository.save(commit);
    }

    private FileChange createFileChange(Commit commit, String filePath, Integer additions, Integer deletions, Integer changes) {
        FileChange fc = new FileChange(
                commit,
                filePath,
                FileChangeStatus.MODIFIED,
                additions,
                deletions,
                changes,
                "https://github.com/blob/" + filePath,
                "https://github.com/raw/" + filePath
        );
        return fileChangeJpaRepository.save(fc);
    }
}
