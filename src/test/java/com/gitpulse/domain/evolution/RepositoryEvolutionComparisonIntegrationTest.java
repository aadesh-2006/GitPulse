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

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RepositoryEvolutionComparisonIntegrationTest {

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
    @DisplayName("GET /evolution/compare - Comprehensive period comparison with all metrics, classifications, positive/negative/zero deltas, and contributor normalization")
    void compareEvolution_ComprehensiveScenario() throws Exception {
        // --- PREVIOUS PERIOD (2026-01-01 to 2026-02-01) ---
        // Prev commit 1: Alice (feature) with 2 file changes
        Commit prev1 = createCommit(repo1, "sha_p1", "feat: initial", "Alice", "alice@example.com", "alice",
                Instant.parse("2026-01-05T10:00:00Z"), 60, 20, 80, CommitClassification.FEATURE);
        createFileChange(prev1, "src/Auth.java", 40, 10, 50);
        createFileChange(prev1, "src/User.java", 20, 10, 30);

        // Prev commit 2: Alice normalized uppercase/spaces (bug_fix)
        Commit prev2 = createCommit(repo1, "sha_p2", "fix: auth bug", "Alice", "  ALICE@example.com  ", "alice",
                Instant.parse("2026-01-15T14:00:00Z"), 10, 5, 15, CommitClassification.BUG_FIX);
        createFileChange(prev2, "src/Auth.java", 10, 5, 15);

        // Prev commit 3: Bob (documentation)
        Commit prev3 = createCommit(repo1, "sha_p3", "docs: readme", "Bob", "bob@example.com", "bob",
                Instant.parse("2026-01-20T11:00:00Z"), 30, 0, 30, CommitClassification.DOCUMENTATION);
        createFileChange(prev3, "README.md", 30, 0, 30);

        // Prev totals:
        // commits: 3 (feat:1, bug:1, docs:1, others:0)
        // adds: 100, dels: 25, churn: 125
        // contributors: 2 (Alice, Bob)
        // filesChanged: 3 (Auth.java, User.java, README.md)

        // --- CURRENT PERIOD (2026-02-01 to 2026-03-01) ---
        // Curr commit 1: Charlie (refactor)
        Commit curr1 = createCommit(repo1, "sha_c1", "refactor: clean auth", "Charlie", "charlie@example.com", "charlie",
                Instant.parse("2026-02-02T09:00:00Z"), 20, 15, 35, CommitClassification.REFACTOR);
        createFileChange(curr1, "src/Auth.java", 20, 15, 35);

        // Curr commit 2: Charlie (test)
        Commit curr2 = createCommit(repo1, "sha_c2", "test: auth test", "Charlie", "charlie@example.com", "charlie",
                Instant.parse("2026-02-05T12:00:00Z"), 50, 5, 55, CommitClassification.TEST);
        createFileChange(curr2, "src/AuthTest.java", 50, 5, 55);

        // Curr commit 3: DevOps (build)
        Commit curr3 = createCommit(repo1, "sha_c3", "build: maven pom", "DevOps", "devops@example.com", "devops",
                Instant.parse("2026-02-10T16:00:00Z"), 10, 2, 12, CommitClassification.BUILD);
        createFileChange(curr3, "pom.xml", 10, 2, 12);

        // Curr commit 4: DevOps (config)
        Commit curr4 = createCommit(repo1, "sha_c4", "config: app yaml", "DevOps", "devops@example.com", "devops",
                Instant.parse("2026-02-15T08:00:00Z"), 8, 4, 12, CommitClassification.CONFIGURATION);
        createFileChange(curr4, "application.yml", 8, 4, 12);

        // Curr commit 5: Bot (dependency)
        Commit curr5 = createCommit(repo1, "sha_c5", "deps: bump lib", "Bot", "bot@example.com", "bot",
                Instant.parse("2026-02-20T10:00:00Z"), 5, 5, 10, CommitClassification.DEPENDENCY);
        createFileChange(curr5, "pom.xml", 5, 5, 10);

        // Curr commit 6: Bob (other)
        Commit curr6 = createCommit(repo1, "sha_c6", "chore: tidy", "Bob", "bob@example.com", "bob",
                Instant.parse("2026-02-25T14:00:00Z"), 4, 4, 8, CommitClassification.OTHER);
        createFileChange(curr6, "src/User.java", 4, 4, 8);

        // Curr commit 7: Bob (unclassified - NULL classification)
        Commit curr7 = createCommit(repo1, "sha_c7", "legacy unclassified", "Bob", "bob@example.com", "bob",
                Instant.parse("2026-02-28T18:00:00Z"), 15, 5, 20, null);
        createFileChange(curr7, "src/User.java", 15, 5, 20);

        // Curr totals:
        // commits: 7 (feat:0, bug:0, refactor:1, docs:0, test:1, build:1, config:1, deps:1, other:1)
        // adds: 112 (20+50+10+8+5+4+15), dels: 40 (15+5+2+4+5+4+5), churn: 152 (112+40)
        // contributors: 4 (Charlie, DevOps, Bot, Bob)
        // filesChanged: 5 (Auth.java, AuthTest.java, pom.xml, application.yml, User.java)

        // Other repo commit (should be ignored by repo isolation)
        Commit repo2Commit = createCommit(repo2, "sha_r2", "feat: other repo", "Alice", "alice@example.com", "alice",
                Instant.parse("2026-02-10T10:00:00Z"), 500, 100, 600, CommitClassification.FEATURE);
        createFileChange(repo2Commit, "src/Other.java", 500, 100, 600);

        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution/compare", repo1.getId())
                        .param("currentFrom", "2026-02-01T00:00:00Z")
                        .param("currentTo", "2026-03-01T00:00:00Z")
                        .param("previousFrom", "2026-01-01T00:00:00Z")
                        .param("previousTo", "2026-02-01T00:00:00Z")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositoryId", is(repo1.getId().intValue())))
                // Current Period Assertions
                .andExpect(jsonPath("$.currentPeriod.from", is("2026-02-01T00:00:00Z")))
                .andExpect(jsonPath("$.currentPeriod.to", is("2026-03-01T00:00:00Z")))
                .andExpect(jsonPath("$.currentPeriod.totalCommits", is(7)))
                .andExpect(jsonPath("$.currentPeriod.totalAdditions", is(112)))
                .andExpect(jsonPath("$.currentPeriod.totalDeletions", is(40)))
                .andExpect(jsonPath("$.currentPeriod.totalChurn", is(152)))
                .andExpect(jsonPath("$.currentPeriod.activeContributors", is(4)))
                .andExpect(jsonPath("$.currentPeriod.filesChanged", is(5)))
                .andExpect(jsonPath("$.currentPeriod.featureCommits", is(0)))
                .andExpect(jsonPath("$.currentPeriod.bugFixCommits", is(0)))
                .andExpect(jsonPath("$.currentPeriod.refactorCommits", is(1)))
                .andExpect(jsonPath("$.currentPeriod.documentationCommits", is(0)))
                .andExpect(jsonPath("$.currentPeriod.testCommits", is(1)))
                .andExpect(jsonPath("$.currentPeriod.buildCommits", is(1)))
                .andExpect(jsonPath("$.currentPeriod.configurationCommits", is(1)))
                .andExpect(jsonPath("$.currentPeriod.dependencyCommits", is(1)))
                .andExpect(jsonPath("$.currentPeriod.otherCommits", is(1)))
                // Previous Period Assertions
                .andExpect(jsonPath("$.previousPeriod.from", is("2026-01-01T00:00:00Z")))
                .andExpect(jsonPath("$.previousPeriod.to", is("2026-02-01T00:00:00Z")))
                .andExpect(jsonPath("$.previousPeriod.totalCommits", is(3)))
                .andExpect(jsonPath("$.previousPeriod.totalAdditions", is(100)))
                .andExpect(jsonPath("$.previousPeriod.totalDeletions", is(25)))
                .andExpect(jsonPath("$.previousPeriod.totalChurn", is(125)))
                .andExpect(jsonPath("$.previousPeriod.activeContributors", is(2)))
                .andExpect(jsonPath("$.previousPeriod.filesChanged", is(3)))
                .andExpect(jsonPath("$.previousPeriod.featureCommits", is(1)))
                .andExpect(jsonPath("$.previousPeriod.bugFixCommits", is(1)))
                .andExpect(jsonPath("$.previousPeriod.refactorCommits", is(0)))
                .andExpect(jsonPath("$.previousPeriod.documentationCommits", is(1)))
                .andExpect(jsonPath("$.previousPeriod.testCommits", is(0)))
                .andExpect(jsonPath("$.previousPeriod.buildCommits", is(0)))
                .andExpect(jsonPath("$.previousPeriod.configurationCommits", is(0)))
                .andExpect(jsonPath("$.previousPeriod.dependencyCommits", is(0)))
                .andExpect(jsonPath("$.previousPeriod.otherCommits", is(0)))
                // Delta Assertions (current - previous)
                .andExpect(jsonPath("$.delta.totalCommits", is(4)))          // 7 - 3 = +4
                .andExpect(jsonPath("$.delta.totalAdditions", is(12)))        // 112 - 100 = +12
                .andExpect(jsonPath("$.delta.totalDeletions", is(15)))        // 40 - 25 = +15
                .andExpect(jsonPath("$.delta.totalChurn", is(27)))            // 152 - 125 = +27
                .andExpect(jsonPath("$.delta.activeContributors", is(2)))     // 4 - 2 = +2
                .andExpect(jsonPath("$.delta.filesChanged", is(2)))           // 5 - 3 = +2
                .andExpect(jsonPath("$.delta.featureCommits", is(-1)))        // 0 - 1 = -1 (negative delta)
                .andExpect(jsonPath("$.delta.bugFixCommits", is(-1)))         // 0 - 1 = -1
                .andExpect(jsonPath("$.delta.refactorCommits", is(1)))        // 1 - 0 = +1
                .andExpect(jsonPath("$.delta.documentationCommits", is(-1)))  // 0 - 1 = -1
                .andExpect(jsonPath("$.delta.testCommits", is(1)))            // 1 - 0 = +1
                .andExpect(jsonPath("$.delta.buildCommits", is(1)))           // 1 - 0 = +1
                .andExpect(jsonPath("$.delta.configurationCommits", is(1)))   // 1 - 0 = +1
                .andExpect(jsonPath("$.delta.dependencyCommits", is(1)))      // 1 - 0 = +1
                .andExpect(jsonPath("$.delta.otherCommits", is(1)));          // 1 - 0 = +1
    }

    @Test
    @DisplayName("GET /evolution/compare - Single commit with multiple file changes does not multiply commit metrics")
    void compareEvolution_MultipleFileChanges_DoesNotMultiplyCommitMetrics() throws Exception {
        Commit curr = createCommit(repo1, "sha_multi_curr", "feat: multi file", "Alice", "alice@example.com", "alice",
                Instant.parse("2026-02-10T10:00:00Z"), 100, 50, 150, CommitClassification.FEATURE);
        createFileChange(curr, "src/A.java", 40, 20, 60);
        createFileChange(curr, "src/B.java", 30, 20, 50);
        createFileChange(curr, "src/C.java", 30, 10, 40);

        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution/compare", repo1.getId())
                        .param("currentFrom", "2026-02-01T00:00:00Z")
                        .param("currentTo", "2026-03-01T00:00:00Z")
                        .param("previousFrom", "2026-01-01T00:00:00Z")
                        .param("previousTo", "2026-02-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPeriod.totalCommits", is(1)))
                .andExpect(jsonPath("$.currentPeriod.totalAdditions", is(100)))
                .andExpect(jsonPath("$.currentPeriod.totalDeletions", is(50)))
                .andExpect(jsonPath("$.currentPeriod.totalChurn", is(150)))
                .andExpect(jsonPath("$.currentPeriod.activeContributors", is(1)))
                .andExpect(jsonPath("$.currentPeriod.filesChanged", is(3)))
                .andExpect(jsonPath("$.delta.totalCommits", is(1)))
                .andExpect(jsonPath("$.delta.totalAdditions", is(100)))
                .andExpect(jsonPath("$.delta.filesChanged", is(3)));
    }

    @Test
    @DisplayName("GET /evolution/compare - Exact boundary semantics: from inclusive, to exclusive for both periods")
    void compareEvolution_BoundarySemantics() throws Exception {
        // Prev exactly at previousFrom -> included in previous
        createCommit(repo1, "sha_pf", "msg", "A", "a@example.com", "a",
                Instant.parse("2026-01-01T00:00:00Z"), 10, 5, 15, CommitClassification.FEATURE);

        // Commit exactly at previousTo / currentFrom -> included in CURRENT, excluded from PREVIOUS
        createCommit(repo1, "sha_pt_cf", "msg", "A", "a@example.com", "a",
                Instant.parse("2026-02-01T00:00:00Z"), 20, 10, 30, CommitClassification.FEATURE);

        // Commit exactly at currentTo -> excluded from CURRENT
        createCommit(repo1, "sha_ct", "msg", "A", "a@example.com", "a",
                Instant.parse("2026-03-01T00:00:00Z"), 30, 15, 45, CommitClassification.FEATURE);

        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution/compare", repo1.getId())
                        .param("currentFrom", "2026-02-01T00:00:00Z")
                        .param("currentTo", "2026-03-01T00:00:00Z")
                        .param("previousFrom", "2026-01-01T00:00:00Z")
                        .param("previousTo", "2026-02-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.previousPeriod.totalCommits", is(1)))
                .andExpect(jsonPath("$.previousPeriod.totalChurn", is(15)))
                .andExpect(jsonPath("$.currentPeriod.totalCommits", is(1)))
                .andExpect(jsonPath("$.currentPeriod.totalChurn", is(30)))
                .andExpect(jsonPath("$.delta.totalCommits", is(0)))      // 1 - 1 = 0
                .andExpect(jsonPath("$.delta.totalChurn", is(15)));      // 30 - 15 = +15
    }

    @Test
    @DisplayName("GET /evolution/compare - Empty current period, empty previous period, both periods empty, and unequal lengths")
    void compareEvolution_EmptyPeriodsAndUnequalLengths() throws Exception {
        // 1. Both periods empty
        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution/compare", repo1.getId())
                        .param("currentFrom", "2026-05-01T00:00:00Z")
                        .param("currentTo", "2026-06-01T00:00:00Z")
                        .param("previousFrom", "2026-01-01T00:00:00Z")
                        .param("previousTo", "2026-03-01T00:00:00Z")) // unequal lengths: 1 month vs 2 months
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPeriod.totalCommits", is(0)))
                .andExpect(jsonPath("$.previousPeriod.totalCommits", is(0)))
                .andExpect(jsonPath("$.delta.totalCommits", is(0)));

        // Add commit to current period only
        createCommit(repo1, "sha_only_curr", "msg", "A", "a@example.com", "a",
                Instant.parse("2026-05-15T00:00:00Z"), 10, 5, 15, CommitClassification.FEATURE);

        // 2. Empty previous period, active current period
        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution/compare", repo1.getId())
                        .param("currentFrom", "2026-05-01T00:00:00Z")
                        .param("currentTo", "2026-06-01T00:00:00Z")
                        .param("previousFrom", "2026-01-01T00:00:00Z")
                        .param("previousTo", "2026-02-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPeriod.totalCommits", is(1)))
                .andExpect(jsonPath("$.previousPeriod.totalCommits", is(0)))
                .andExpect(jsonPath("$.delta.totalCommits", is(1)));

        // 3. Active previous period, empty current period
        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution/compare", repo1.getId())
                        .param("currentFrom", "2026-07-01T00:00:00Z")
                        .param("currentTo", "2026-08-01T00:00:00Z")
                        .param("previousFrom", "2026-05-01T00:00:00Z")
                        .param("previousTo", "2026-06-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPeriod.totalCommits", is(0)))
                .andExpect(jsonPath("$.previousPeriod.totalCommits", is(1)))
                .andExpect(jsonPath("$.delta.totalCommits", is(-1)));
    }

    @Test
    @DisplayName("GET /evolution/compare - Validation errors return 400 Bad Request")
    void compareEvolution_ValidationFailures_Returns400() throws Exception {
        // currentFrom >= currentTo
        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution/compare", repo1.getId())
                        .param("currentFrom", "2026-02-01T00:00:00Z")
                        .param("currentTo", "2026-02-01T00:00:00Z")
                        .param("previousFrom", "2026-01-01T00:00:00Z")
                        .param("previousTo", "2026-02-01T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("The 'currentFrom' timestamp must be strictly before the 'currentTo' timestamp")));

        // previousFrom >= previousTo
        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution/compare", repo1.getId())
                        .param("currentFrom", "2026-02-01T00:00:00Z")
                        .param("currentTo", "2026-03-01T00:00:00Z")
                        .param("previousFrom", "2026-02-01T00:00:00Z")
                        .param("previousTo", "2026-01-01T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("The 'previousFrom' timestamp must be strictly before the 'previousTo' timestamp")));

        // missing currentFrom
        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution/compare", repo1.getId())
                        .param("currentTo", "2026-03-01T00:00:00Z")
                        .param("previousFrom", "2026-01-01T00:00:00Z")
                        .param("previousTo", "2026-02-01T00:00:00Z"))
                .andExpect(status().isBadRequest());

        // missing currentTo
        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution/compare", repo1.getId())
                        .param("currentFrom", "2026-02-01T00:00:00Z")
                        .param("previousFrom", "2026-01-01T00:00:00Z")
                        .param("previousTo", "2026-02-01T00:00:00Z"))
                .andExpect(status().isBadRequest());

        // missing previousFrom
        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution/compare", repo1.getId())
                        .param("currentFrom", "2026-02-01T00:00:00Z")
                        .param("currentTo", "2026-03-01T00:00:00Z")
                        .param("previousTo", "2026-02-01T00:00:00Z"))
                .andExpect(status().isBadRequest());

        // missing previousTo
        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution/compare", repo1.getId())
                        .param("currentFrom", "2026-02-01T00:00:00Z")
                        .param("currentTo", "2026-03-01T00:00:00Z")
                        .param("previousFrom", "2026-01-01T00:00:00Z"))
                .andExpect(status().isBadRequest());

        // malformed timestamp
        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution/compare", repo1.getId())
                        .param("currentFrom", "not-a-timestamp")
                        .param("currentTo", "2026-03-01T00:00:00Z")
                        .param("previousFrom", "2026-01-01T00:00:00Z")
                        .param("previousTo", "2026-02-01T00:00:00Z"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /evolution/compare - Non-existent repository returns 404 Not Found")
    void compareEvolution_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/evolution/compare", 99999L)
                        .param("currentFrom", "2026-02-01T00:00:00Z")
                        .param("currentTo", "2026-03-01T00:00:00Z")
                        .param("previousFrom", "2026-01-01T00:00:00Z")
                        .param("previousTo", "2026-02-01T00:00:00Z"))
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
