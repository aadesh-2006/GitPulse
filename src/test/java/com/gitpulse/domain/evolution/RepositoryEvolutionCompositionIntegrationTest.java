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

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RepositoryEvolutionCompositionIntegrationTest {

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
    @DisplayName("GET /evolution/composition - Comprehensive scenario with all 9 classifications, NULL unclassified handling, category shares, and intensity averages")
    void getEvolutionComposition_ComprehensiveScenario() throws Exception {
        // 1. FEATURE (Alice) with 2 file changes
        Commit c1 = createCommit(repo1, "sha_c1", "feat: auth", "Alice", "alice@example.com", "alice",
                Instant.parse("2026-01-05T10:00:00Z"), 60, 20, 80, CommitClassification.FEATURE);
        createFileChange(c1, "src/Auth.java", 40, 10, 50);
        createFileChange(c1, "src/User.java", 20, 10, 30);

        // 2. BUG_FIX (Alice normalized with spaces/caps) - modifies Auth.java again (duplicate distinct check)
        Commit c2 = createCommit(repo1, "sha_c2", "fix: bug", "Alice", "  ALICE@example.com  ", "alice",
                Instant.parse("2026-01-08T12:00:00Z"), 10, 5, 15, CommitClassification.BUG_FIX);
        createFileChange(c2, "src/Auth.java", 10, 5, 15);

        // 3. REFACTOR (Bob)
        Commit c3 = createCommit(repo1, "sha_c3", "refactor: clean", "Bob", "bob@example.com", "bob",
                Instant.parse("2026-01-12T09:00:00Z"), 30, 10, 40, CommitClassification.REFACTOR);
        createFileChange(c3, "src/Token.java", 30, 10, 40);

        // 4. DOCUMENTATION (Charlie)
        Commit c4 = createCommit(repo1, "sha_c4", "docs: guide", "Charlie", "charlie@example.com", "charlie",
                Instant.parse("2026-01-15T11:00:00Z"), 20, 0, 20, CommitClassification.DOCUMENTATION);
        createFileChange(c4, "README.md", 20, 0, 20);

        // 5. TEST (Charlie)
        Commit c5 = createCommit(repo1, "sha_c5", "test: integration", "Charlie", "charlie@example.com", "charlie",
                Instant.parse("2026-01-18T14:00:00Z"), 40, 0, 40, CommitClassification.TEST);
        createFileChange(c5, "src/test/AuthTest.java", 40, 0, 40);

        // 6. BUILD (DevOps)
        Commit c6 = createCommit(repo1, "sha_c6", "build: pom", "DevOps", "devops@example.com", "devops",
                Instant.parse("2026-01-20T16:00:00Z"), 10, 5, 15, CommitClassification.BUILD);
        createFileChange(c6, "pom.xml", 10, 5, 15);

        // 7. CONFIGURATION (DevOps)
        Commit c7 = createCommit(repo1, "sha_c7", "config: app.yml", "DevOps", "devops@example.com", "devops",
                Instant.parse("2026-01-22T08:00:00Z"), 5, 2, 7, CommitClassification.CONFIGURATION);
        createFileChange(c7, "application.yml", 5, 2, 7);

        // 8. DEPENDENCY (Bot)
        Commit c8 = createCommit(repo1, "sha_c8", "deps: bump lib", "Bot", "bot@example.com", "bot",
                Instant.parse("2026-01-25T10:00:00Z"), 5, 5, 10, CommitClassification.DEPENDENCY);
        createFileChange(c8, "pom.xml", 5, 5, 10);

        // 9. OTHER (Bob)
        Commit c9 = createCommit(repo1, "sha_c9", "chore: misc", "Bob", "bob@example.com", "bob",
                Instant.parse("2026-01-28T14:00:00Z"), 10, 3, 13, CommitClassification.OTHER);
        createFileChange(c9, "src/Misc.java", 10, 3, 13);

        // 10. UNCLASSIFIED (Bob, classification = NULL)
        Commit c10 = createCommit(repo1, "sha_c10", "legacy unclassified", "Bob", "bob@example.com", "bob",
                Instant.parse("2026-01-30T18:00:00Z"), 10, 0, 10, null);
        createFileChange(c10, "src/Legacy.java", 10, 0, 10);

        // Other repo commit (should be ignored by repo isolation)
        Commit repo2Commit = createCommit(repo2, "sha_repo2", "feat: other", "Alice", "alice@example.com", "alice",
                Instant.parse("2026-01-10T10:00:00Z"), 100, 50, 150, CommitClassification.FEATURE);
        createFileChange(repo2Commit, "src/Other.java", 100, 50, 150);

        // Totals:
        // totalCommits = 10
        // classifiedCommits = 9 (1 each of all 9 types)
        // unclassifiedCommits = 1 (c10 with NULL classification)
        // totalAdditions = 60+10+30+20+40+10+5+5+10+10 = 200
        // totalDeletions = 20+5+10+0+0+5+2+5+3+0 = 50
        // totalChurn = 200 + 50 = 250
        // activeContributors = 5 (Alice, Bob, Charlie, DevOps, Bot)
        // filesChanged = 9 distinct (Auth.java, User.java, Token.java, README.md, AuthTest.java, pom.xml, application.yml, Misc.java, Legacy.java)

        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution/composition", repo1.getId())
                        .param("from", "2026-01-01T00:00:00Z")
                        .param("to", "2026-02-01T00:00:00Z")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositoryId", is(repo1.getId().intValue())))
                .andExpect(jsonPath("$.from", is("2026-01-01T00:00:00Z")))
                .andExpect(jsonPath("$.to", is("2026-02-01T00:00:00Z")))
                // Raw metrics
                .andExpect(jsonPath("$.rawMetrics.totalCommits", is(10)))
                .andExpect(jsonPath("$.rawMetrics.totalAdditions", is(200)))
                .andExpect(jsonPath("$.rawMetrics.totalDeletions", is(50)))
                .andExpect(jsonPath("$.rawMetrics.totalChurn", is(250)))
                .andExpect(jsonPath("$.rawMetrics.activeContributors", is(5)))
                .andExpect(jsonPath("$.rawMetrics.filesChanged", is(9)))
                .andExpect(jsonPath("$.rawMetrics.featureCommits", is(1)))
                .andExpect(jsonPath("$.rawMetrics.bugFixCommits", is(1)))
                .andExpect(jsonPath("$.rawMetrics.refactorCommits", is(1)))
                .andExpect(jsonPath("$.rawMetrics.documentationCommits", is(1)))
                .andExpect(jsonPath("$.rawMetrics.testCommits", is(1)))
                .andExpect(jsonPath("$.rawMetrics.buildCommits", is(1)))
                .andExpect(jsonPath("$.rawMetrics.configurationCommits", is(1)))
                .andExpect(jsonPath("$.rawMetrics.dependencyCommits", is(1)))
                .andExpect(jsonPath("$.rawMetrics.otherCommits", is(1)))
                // Composition metrics
                .andExpect(jsonPath("$.composition.classifiedCommits", is(9)))
                .andExpect(jsonPath("$.composition.unclassifiedCommits", is(1))) // NULL commit is unclassified
                .andExpect(jsonPath("$.composition.featureShare", is(closeTo(1.0 / 9.0, 0.0001))))
                .andExpect(jsonPath("$.composition.bugFixShare", is(closeTo(1.0 / 9.0, 0.0001))))
                .andExpect(jsonPath("$.composition.refactorShare", is(closeTo(1.0 / 9.0, 0.0001))))
                .andExpect(jsonPath("$.composition.documentationShare", is(closeTo(1.0 / 9.0, 0.0001))))
                .andExpect(jsonPath("$.composition.testShare", is(closeTo(1.0 / 9.0, 0.0001))))
                .andExpect(jsonPath("$.composition.buildShare", is(closeTo(1.0 / 9.0, 0.0001))))
                .andExpect(jsonPath("$.composition.configurationShare", is(closeTo(1.0 / 9.0, 0.0001))))
                .andExpect(jsonPath("$.composition.dependencyShare", is(closeTo(1.0 / 9.0, 0.0001))))
                .andExpect(jsonPath("$.composition.otherShare", is(closeTo(1.0 / 9.0, 0.0001)))) // otherShare is 1/9, NOT 2/9 (NULL is excluded)
                // Intensity metrics
                .andExpect(jsonPath("$.intensity.averageChurnPerCommit", is(closeTo(25.0, 0.0001))))         // 250 / 10
                .andExpect(jsonPath("$.intensity.averageFilesChangedPerCommit", is(closeTo(0.9, 0.0001))))  // 9 / 10
                .andExpect(jsonPath("$.intensity.averageAdditionsPerCommit", is(closeTo(20.0, 0.0001))))     // 200 / 10
                .andExpect(jsonPath("$.intensity.averageDeletionsPerCommit", is(closeTo(5.0, 0.0001))));     // 50 / 10
    }

    @Test
    @DisplayName("GET /evolution/composition - Single commit with multiple file changes does not multiply commit totals")
    void getEvolutionComposition_MultipleFileChanges_DoesNotMultiplyTotals() throws Exception {
        Commit c = createCommit(repo1, "sha_multi", "feat: multi files", "Alice", "alice@example.com", "alice",
                Instant.parse("2026-01-10T10:00:00Z"), 100, 50, 150, CommitClassification.FEATURE);
        createFileChange(c, "src/A.java", 40, 20, 60);
        createFileChange(c, "src/B.java", 30, 20, 50);
        createFileChange(c, "src/C.java", 30, 10, 40);

        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution/composition", repo1.getId())
                        .param("from", "2026-01-01T00:00:00Z")
                        .param("to", "2026-02-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rawMetrics.totalCommits", is(1)))
                .andExpect(jsonPath("$.rawMetrics.totalAdditions", is(100)))
                .andExpect(jsonPath("$.rawMetrics.totalDeletions", is(50)))
                .andExpect(jsonPath("$.rawMetrics.totalChurn", is(150)))
                .andExpect(jsonPath("$.rawMetrics.filesChanged", is(3)))
                .andExpect(jsonPath("$.intensity.averageFilesChangedPerCommit", is(closeTo(3.0, 0.0001))))
                .andExpect(jsonPath("$.intensity.averageChurnPerCommit", is(closeTo(150.0, 0.0001))));
    }

    @Test
    @DisplayName("GET /evolution/composition - Boundary inclusiveness on from and exclusiveness on to")
    void getEvolutionComposition_BoundarySemantics() throws Exception {
        // Exactly at from -> included
        createCommit(repo1, "sha_boundary_from", "msg", "A", "a@example.com", "a",
                Instant.parse("2026-01-01T00:00:00Z"), 10, 5, 15, CommitClassification.FEATURE);

        // Exactly at to -> excluded
        createCommit(repo1, "sha_boundary_to", "msg", "A", "a@example.com", "a",
                Instant.parse("2026-02-01T00:00:00Z"), 20, 10, 30, CommitClassification.FEATURE);

        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution/composition", repo1.getId())
                        .param("from", "2026-01-01T00:00:00Z")
                        .param("to", "2026-02-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rawMetrics.totalCommits", is(1)))
                .andExpect(jsonPath("$.rawMetrics.totalChurn", is(15)));
    }

    @Test
    @DisplayName("GET /evolution/composition - Empty period returns zero for all counts, shares, and intensity metrics")
    void getEvolutionComposition_EmptyPeriod_ReturnsZeroes() throws Exception {
        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution/composition", repo1.getId())
                        .param("from", "2026-05-01T00:00:00Z")
                        .param("to", "2026-06-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rawMetrics.totalCommits", is(0)))
                .andExpect(jsonPath("$.rawMetrics.totalAdditions", is(0)))
                .andExpect(jsonPath("$.rawMetrics.totalDeletions", is(0)))
                .andExpect(jsonPath("$.rawMetrics.totalChurn", is(0)))
                .andExpect(jsonPath("$.rawMetrics.activeContributors", is(0)))
                .andExpect(jsonPath("$.rawMetrics.filesChanged", is(0)))
                .andExpect(jsonPath("$.composition.classifiedCommits", is(0)))
                .andExpect(jsonPath("$.composition.unclassifiedCommits", is(0)))
                .andExpect(jsonPath("$.composition.featureShare", is(0.0)))
                .andExpect(jsonPath("$.composition.bugFixShare", is(0.0)))
                .andExpect(jsonPath("$.composition.otherShare", is(0.0)))
                .andExpect(jsonPath("$.intensity.averageChurnPerCommit", is(0.0)))
                .andExpect(jsonPath("$.intensity.averageFilesChangedPerCommit", is(0.0)))
                .andExpect(jsonPath("$.intensity.averageAdditionsPerCommit", is(0.0)))
                .andExpect(jsonPath("$.intensity.averageDeletionsPerCommit", is(0.0)));
    }

    @Test
    @DisplayName("GET /evolution/composition - Period with only unclassified commits produces 0 classifiedCommits and 0.0 shares")
    void getEvolutionComposition_OnlyUnclassifiedCommits_ReturnsZeroShares() throws Exception {
        createCommit(repo1, "sha_unclassified_only", "msg", "A", "a@example.com", "a",
                Instant.parse("2026-01-10T10:00:00Z"), 20, 10, 30, null);

        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution/composition", repo1.getId())
                        .param("from", "2026-01-01T00:00:00Z")
                        .param("to", "2026-02-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rawMetrics.totalCommits", is(1)))
                .andExpect(jsonPath("$.composition.classifiedCommits", is(0)))
                .andExpect(jsonPath("$.composition.unclassifiedCommits", is(1)))
                .andExpect(jsonPath("$.composition.featureShare", is(0.0)))
                .andExpect(jsonPath("$.composition.otherShare", is(0.0)))
                .andExpect(jsonPath("$.intensity.averageChurnPerCommit", is(closeTo(30.0, 0.0001))));
    }

    @Test
    @DisplayName("GET /evolution/composition - Validation errors return 400 Bad Request")
    void getEvolutionComposition_ValidationErrors_Returns400() throws Exception {
        // from >= to
        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution/composition", repo1.getId())
                        .param("from", "2026-02-01T00:00:00Z")
                        .param("to", "2026-02-01T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("The 'from' timestamp must be strictly before the 'to' timestamp")));

        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution/composition", repo1.getId())
                        .param("from", "2026-03-01T00:00:00Z")
                        .param("to", "2026-02-01T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("The 'from' timestamp must be strictly before the 'to' timestamp")));

        // missing from
        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution/composition", repo1.getId())
                        .param("to", "2026-02-01T00:00:00Z"))
                .andExpect(status().isBadRequest());

        // missing to
        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution/composition", repo1.getId())
                        .param("from", "2026-01-01T00:00:00Z"))
                .andExpect(status().isBadRequest());

        // malformed timestamp
        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution/composition", repo1.getId())
                        .param("from", "invalid-instant")
                        .param("to", "2026-02-01T00:00:00Z"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /evolution/composition - Non-existent repository returns 404 Not Found")
    void getEvolutionComposition_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution/composition", 99999L)
                        .param("from", "2026-01-01T00:00:00Z")
                        .param("to", "2026-02-01T00:00:00Z"))
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
