package com.gitpulse.benchmark;

import com.gitpulse.benchmark.dto.FutureFileOutcomeRow;
import com.gitpulse.benchmark.dto.HistoricalFileSnapshotRow;
import com.gitpulse.benchmark.repository.BenchmarkQueryRepository;
import com.gitpulse.domain.commit.Commit;
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
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class BenchmarkTemporalBoundaryTest {

    @Autowired
    private RepositoryJpaRepository repositoryJpaRepository;

    @Autowired
    private CommitJpaRepository commitJpaRepository;

    @Autowired
    private FileChangeJpaRepository fileChangeJpaRepository;

    @Autowired
    private BenchmarkQueryRepository benchmarkQueryRepository;

    private Repository repo;
    private final Instant cutoff = Instant.parse("2025-06-01T12:00:00Z");
    private final Duration horizon = Duration.ofDays(30);
    private final Instant horizonEnd = cutoff.plus(horizon); // 2025-07-01T12:00:00Z

    @BeforeEach
    void setUp() {
        repo = repositoryJpaRepository.save(new Repository("owner", "boundary-repo", "Boundary Test Repo", "main"));
    }

    @Test
    @DisplayName("Historical snapshot boundary: inclusive of <= cutoff, exclusive of > cutoff")
    void historicalSnapshotBoundarySemantics() {
        // Commit 1: 1 second before cutoff -> INCLUDED in historical
        createCommitWithChange("sha-before", cutoff.minusSeconds(1), "src/Before.java", 10, 5);

        // Commit 2: Exactly at cutoff -> INCLUDED in historical
        createCommitWithChange("sha-exact", cutoff, "src/Exact.java", 20, 10);

        // Commit 3: 1 second after cutoff -> EXCLUDED from historical
        createCommitWithChange("sha-after", cutoff.plusSeconds(1), "src/After.java", 30, 15);

        List<HistoricalFileSnapshotRow> snapshots = benchmarkQueryRepository.findHistoricalFileSnapshots(repo.getId(), cutoff);
        Map<String, HistoricalFileSnapshotRow> map = snapshots.stream()
                .collect(Collectors.toMap(HistoricalFileSnapshotRow::getFilePath, r -> r));

        assertThat(map).containsKey("src/Before.java");
        assertThat(map).containsKey("src/Exact.java");
        assertThat(map).doesNotContainKey("src/After.java");

        assertThat(map.get("src/Before.java").getTotalRevisions()).isEqualTo(1);
        assertThat(map.get("src/Exact.java").getTotalRevisions()).isEqualTo(1);
    }

    @Test
    @DisplayName("Future window boundary: exclusive of <= cutoff, inclusive of <= horizonEnd, exclusive of > horizonEnd")
    void futureWindowBoundarySemantics() {
        // Commit 1: Exactly at cutoff -> EXCLUDED from future
        createCommitWithChange("sha-cutoff", cutoff, "src/AtCutoff.java", 10, 5);

        // Commit 2: 1 second after cutoff -> INCLUDED in future
        createCommitWithChange("sha-fut-start", cutoff.plusSeconds(1), "src/FutureStart.java", 20, 10);

        // Commit 3: Exactly at horizon end -> INCLUDED in future
        createCommitWithChange("sha-fut-end", horizonEnd, "src/FutureEnd.java", 30, 15);

        // Commit 4: 1 second after horizon end -> EXCLUDED from future
        createCommitWithChange("sha-fut-after", horizonEnd.plusSeconds(1), "src/FutureAfter.java", 40, 20);

        List<FutureFileOutcomeRow> outcomes = benchmarkQueryRepository.findFutureFileOutcomes(repo.getId(), cutoff, horizonEnd);
        Map<String, FutureFileOutcomeRow> map = outcomes.stream()
                .collect(Collectors.toMap(FutureFileOutcomeRow::getFilePath, r -> r));

        assertThat(map).doesNotContainKey("src/AtCutoff.java");
        assertThat(map).containsKey("src/FutureStart.java");
        assertThat(map).containsKey("src/FutureEnd.java");
        assertThat(map).doesNotContainKey("src/FutureAfter.java");

        assertThat(map.get("src/FutureStart.java").getFutureRevisionCount()).isEqualTo(1);
        assertThat(map.get("src/FutureEnd.java").getFutureRevisionCount()).isEqualTo(1);
    }

    private void createCommitWithChange(String sha, Instant time, String filePath, int additions, int deletions) {
        Commit commit = commitJpaRepository.save(new Commit(
                repo, sha, "Commit " + sha, "Developer", "dev@example.com", "dev",
                time, additions, deletions, additions + deletions, "https://github.com/test/" + sha
        ));
        fileChangeJpaRepository.save(new FileChange(
                commit, filePath, FileChangeStatus.MODIFIED, additions, deletions, additions + deletions, null, null
        ));
        commitJpaRepository.flush();
        fileChangeJpaRepository.flush();
    }
}
