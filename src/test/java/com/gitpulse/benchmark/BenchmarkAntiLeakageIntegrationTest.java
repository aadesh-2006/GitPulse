package com.gitpulse.benchmark;

import com.gitpulse.benchmark.dto.BenchmarkRankingMetrics;
import com.gitpulse.benchmark.dto.BenchmarkResult;
import com.gitpulse.benchmark.dto.HistoricalFileSnapshotRow;
import com.gitpulse.benchmark.repository.BenchmarkQueryRepository;
import com.gitpulse.benchmark.service.BenchmarkService;
import com.gitpulse.domain.commit.Commit;
import com.gitpulse.domain.commit.CommitJpaRepository;
import com.gitpulse.domain.filechange.FileChange;
import com.gitpulse.domain.filechange.FileChangeJpaRepository;
import com.gitpulse.domain.filechange.FileChangeStatus;
import com.gitpulse.domain.repository.Repository;
import com.gitpulse.domain.repository.RepositoryJpaRepository;
import com.gitpulse.domain.risk.FileRiskScoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DataJpaTest
@ActiveProfiles("test")
@Import({BenchmarkService.class, FileRiskScoringService.class})
class BenchmarkAntiLeakageIntegrationTest {

    @Autowired
    private BenchmarkService benchmarkService;

    @Autowired
    private BenchmarkQueryRepository benchmarkQueryRepository;

    @Autowired
    private RepositoryJpaRepository repositoryJpaRepository;

    @Autowired
    private CommitJpaRepository commitJpaRepository;

    @Autowired
    private FileChangeJpaRepository fileChangeJpaRepository;

    private Repository repo;
    private final Instant cutoff = Instant.parse("2025-06-01T00:00:00Z");
    private final Duration horizon = Duration.ofDays(30);

    @BeforeEach
    void setUp() {
        repo = repositoryJpaRepository.save(new Repository("owner", "anti-leakage-repo", "Anti Leakage Repo", "main"));
    }

    @Test
    @DisplayName("Anti-Leakage Invariant: Future commits and contributors do not alter historical snapshots or scores")
    void futureActivityDoesNotLeakIntoHistoricalScores() {
        // 1. Seed historical commits up to cutoff
        Instant t1 = cutoff.minus(Duration.ofDays(10));
        Instant t2 = cutoff.minus(Duration.ofDays(5));

        createCommit("sha-h1", "Alice", "alice@example.com", t1, "src/Hotspot.java", 50, 10);
        createCommit("sha-h2", "Alice", "alice@example.com", t2, "src/Hotspot.java", 30, 5);
        createCommit("sha-h3", "Bob", "bob@example.com", t1, "src/Stable.java", 10, 2);

        // First benchmark evaluation (before future commits exist)
        BenchmarkResult resultBeforeFuture = benchmarkService.evaluateRepository(repo.getId(), cutoff, horizon);
        List<HistoricalFileSnapshotRow> snapshotsBefore = benchmarkQueryRepository.findHistoricalFileSnapshots(repo.getId(), cutoff);

        // 2. Add massive future activity AFTER cutoff
        Instant tFuture1 = cutoff.plus(Duration.ofDays(2));
        Instant tFuture2 = cutoff.plus(Duration.ofDays(15));

        // Charlie (new future contributor) makes massive changes to Stable.java in future window
        createCommit("sha-f1", "Charlie", "charlie@example.com", tFuture1, "src/Stable.java", 10000, 5000);
        createCommit("sha-f2", "Charlie", "charlie@example.com", tFuture2, "src/NewFutureFile.java", 500, 100);

        // Second benchmark evaluation (after future activity exists)
        BenchmarkResult resultAfterFuture = benchmarkService.evaluateRepository(repo.getId(), cutoff, horizon);
        List<HistoricalFileSnapshotRow> snapshotsAfter = benchmarkQueryRepository.findHistoricalFileSnapshots(repo.getId(), cutoff);

        // Verify that historical snapshots are strictly invariant
        assertThat(snapshotsAfter).hasSize(snapshotsBefore.size());
        for (int i = 0; i < snapshotsBefore.size(); i++) {
            HistoricalFileSnapshotRow before = snapshotsBefore.get(i);
            HistoricalFileSnapshotRow after = snapshotsAfter.get(i);
            assertThat(after.getFilePath()).isEqualTo(before.getFilePath());
            assertThat(after.getTotalRevisions()).isEqualTo(before.getTotalRevisions());
            assertThat(after.getTotalChurn()).isEqualTo(before.getTotalChurn());
            assertThat(after.getLastModifiedAtInstant()).isEqualTo(before.getLastModifiedAtInstant());
        }

        // Verify that historical file count at cutoff remains identical
        assertThat(resultAfterFuture.totalHistoricalFiles()).isEqualTo(resultBeforeFuture.totalHistoricalFiles());

        // Verify that future outcomes changed as expected
        assertThat(resultBeforeFuture.totalActiveFutureFiles()).isEqualTo(0);
        assertThat(resultBeforeFuture.futureChangedHistoricalFiles()).isEqualTo(0);

        assertThat(resultAfterFuture.totalActiveFutureFiles()).isEqualTo(2); // Stable.java and NewFutureFile.java
        assertThat(resultAfterFuture.futureChangedHistoricalFiles()).isEqualTo(1); // Stable.java
        assertThat(resultAfterFuture.newFilesIntroducedInFuture()).isEqualTo(1); // NewFutureFile.java
    }

    @Test
    @DisplayName("End-to-end Benchmark Evaluation produces complete, bounded metrics and ablation sets")
    void fullBenchmarkEvaluationExecution() {
        Instant tOld = cutoff.minus(Duration.ofDays(60));
        Instant tRecent = cutoff.minus(Duration.ofDays(2));

        // File A: High historical revisions and churn, modified in future
        createCommit("sha-a1", "Alice", "alice@example.com", tOld, "src/A.java", 100, 20);
        createCommit("sha-a2", "Alice", "alice@example.com", tRecent, "src/A.java", 80, 10);
        createCommit("sha-a3", "Alice", "alice@example.com", tRecent, "src/A.java", 40, 5);

        // File B: Moderate historical revisions, modified in future
        createCommit("sha-b1", "Bob", "bob@example.com", tRecent, "src/B.java", 30, 10);
        createCommit("sha-b2", "Bob", "bob@example.com", tRecent, "src/B.java", 20, 5);

        // File C: Stale historical file, not modified in future
        createCommit("sha-c1", "Charlie", "charlie@example.com", tOld, "src/C.java", 10, 2);

        // File D: Low revisions, not modified in future
        createCommit("sha-d1", "Dave", "dave@example.com", tOld, "src/D.java", 5, 1);

        // Future window commits: A and B modified
        Instant tFut = cutoff.plus(Duration.ofDays(5));
        createCommit("sha-fa1", "Alice", "alice@example.com", tFut, "src/A.java", 50, 10);
        createCommit("sha-fb1", "Bob", "bob@example.com", tFut, "src/B.java", 15, 5);

        BenchmarkResult result = benchmarkService.evaluateRepository(repo.getId(), cutoff, horizon);

        assertThat(result.repositoryId()).isEqualTo(repo.getId());
        assertThat(result.repositoryName()).isEqualTo(repo.getFullName());
        assertThat(result.cutoffTime()).isEqualTo(cutoff);
        assertThat(result.horizonDuration()).isEqualTo(horizon);
        assertThat(result.totalHistoricalFiles()).isEqualTo(4); // A, B, C, D
        assertThat(result.totalActiveFutureFiles()).isEqualTo(2); // A, B
        assertThat(result.futureChangedHistoricalFiles()).isEqualTo(2); // A, B
        assertThat(result.newFilesIntroducedInFuture()).isEqualTo(0);

        // Check baseline and composite metrics
        BenchmarkRankingMetrics baseline = result.baselineMetrics();
        BenchmarkRankingMetrics composite = result.compositeMetrics();

        assertThat(baseline.signalName()).isEqualTo("baseline");
        assertThat(composite.signalName()).isEqualTo("composite");

        // Verify metric bounds
        for (BenchmarkRankingMetrics m : List.of(baseline, composite)) {
            assertThat(m.precisionAt5()).isBetween(0.0, 1.0);
            assertThat(m.precisionAt10()).isBetween(0.0, 1.0);
            assertThat(m.precisionAt20()).isBetween(0.0, 1.0);
            assertThat(m.recallAt5()).isBetween(0.0, 1.0);
            assertThat(m.recallAt10()).isBetween(0.0, 1.0);
            assertThat(m.recallAt20()).isBetween(0.0, 1.0);
            assertThat(m.hitRateAt5()).isIn(0.0, 1.0);
            assertThat(m.hitRateAt10()).isIn(0.0, 1.0);
            assertThat(m.hitRateAt20()).isIn(0.0, 1.0);
            assertThat(m.spearmanCorrelationRevisions()).isBetween(-1.0, 1.0);
            assertThat(m.spearmanCorrelationChurn()).isBetween(-1.0, 1.0);
            assertThat(m.rocAreaUnderCurve()).isBetween(0.0, 1.0);
        }

        // Ablation set verification
        assertThat(result.ablationMetrics()).containsKeys(
                "baseline", "composite", "revisionFrequency", "churn", "recency", "ownershipConcentration"
        );

        // Comparison summary verification
        assertThat(result.comparisonSummary()).isNotNull();
        assertThat(result.comparisonSummary().rocAucDelta())
                .isCloseTo(composite.rocAreaUnderCurve() - baseline.rocAreaUnderCurve(), within(1e-9));
    }

    private void createCommit(String sha, String authorName, String authorEmail, Instant time, String filePath, int additions, int deletions) {
        Commit commit = commitJpaRepository.save(new Commit(
                repo, sha, "Message " + sha, authorName, authorEmail, authorName.toLowerCase(),
                time, additions, deletions, additions + deletions, "https://github.com/test/" + sha
        ));
        fileChangeJpaRepository.save(new FileChange(
                commit, filePath, FileChangeStatus.MODIFIED, additions, deletions, additions + deletions, null, null
        ));
        commitJpaRepository.flush();
        fileChangeJpaRepository.flush();
    }
}
