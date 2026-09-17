package com.gitpulse.domain.contributorfile;

import com.gitpulse.common.exception.ResourceNotFoundException;
import com.gitpulse.domain.commit.Commit;
import com.gitpulse.domain.commit.CommitJpaRepository;
import com.gitpulse.domain.contributor.Contributor;
import com.gitpulse.domain.contributor.ContributorJpaRepository;
import com.gitpulse.domain.contributorfile.dto.RepositoryContributorFileAggregationResult;
import com.gitpulse.domain.filechange.FileChange;
import com.gitpulse.domain.filechange.FileChangeJpaRepository;
import com.gitpulse.domain.filechange.FileChangeStatus;
import com.gitpulse.domain.repository.Repository;
import com.gitpulse.domain.repository.RepositoryJpaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import(RepositoryContributorFileAggregationService.class)
class RepositoryContributorFileAggregationIntegrationTest {

    @Autowired
    private RepositoryContributorFileAggregationService aggregationService;

    @Autowired
    private RepositoryContributorFileJpaRepository repositoryContributorFileJpaRepository;

    @Autowired
    private RepositoryJpaRepository repositoryJpaRepository;

    @Autowired
    private ContributorJpaRepository contributorJpaRepository;

    @Autowired
    private CommitJpaRepository commitJpaRepository;

    @Autowired
    private FileChangeJpaRepository fileChangeJpaRepository;

    @Autowired
    private EntityManager entityManager;

    private Repository repo1;
    private Repository repo2;
    private Contributor alice;
    private Contributor bob;

    @BeforeEach
    void setUp() {
        repo1 = repositoryJpaRepository.save(new Repository("owner1", "repo1", "Repo 1", "main"));
        repo2 = repositoryJpaRepository.save(new Repository("owner2", "repo2", "Repo 2", "main"));
        alice = contributorJpaRepository.save(new Contributor("alice@corp.com", "alice", "Alice Smith"));
        bob = contributorJpaRepository.save(new Contributor("bob@corp.com", "bob", "Bob Jones"));
    }

    @Test
    @DisplayName("1. One contributor + one file produces correct aggregated metrics")
    void singleContributorSingleFile() {
        Instant t1 = Instant.parse("2026-09-01T10:00:00Z");
        Commit c = commitJpaRepository.save(new Commit(repo1, "sha1", "feat: initial", "Alice", "alice@corp.com", "alice", t1, 10, 2, 12, null));
        fileChangeJpaRepository.save(new FileChange(c, "src/App.java", FileChangeStatus.ADDED, 10, 2, 12, null, null));
        fileChangeJpaRepository.flush();

        RepositoryContributorFileAggregationResult result = aggregationService.aggregateRepositoryContributorFiles(repo1.getId());

        assertThat(result.totalRowsProcessed()).isEqualTo(1);
        assertThat(result.createdCount()).isEqualTo(1);
        assertThat(result.updatedCount()).isEqualTo(0);
        assertThat(result.unchangedCount()).isEqualTo(0);
        assertThat(result.deletedCount()).isEqualTo(0);

        RepositoryContributorFile record = repositoryContributorFileJpaRepository
                .findByRepositoryIdAndContributorIdAndFilePath(repo1.getId(), alice.getId(), "src/App.java")
                .orElseThrow();

        assertThat(record.getTotalRevisions()).isEqualTo(1);
        assertThat(record.getTotalAdditions()).isEqualTo(10);
        assertThat(record.getTotalDeletions()).isEqualTo(2);
        assertThat(record.getTotalChurn()).isEqualTo(12);
        assertThat(record.getFirstContributedAt()).isEqualTo(t1);
        assertThat(record.getLastContributedAt()).isEqualTo(t1);
    }

    @Test
    @DisplayName("2 & 5 & 6. Multiple commits on the same file aggregate additions, deletions, churn, and timestamp bounds")
    void multipleCommitsOnSameFile() {
        Instant t1 = Instant.parse("2026-09-01T10:00:00Z");
        Instant t2 = Instant.parse("2026-09-03T15:00:00Z");
        Instant t3 = Instant.parse("2026-09-05T18:00:00Z");

        Commit c1 = commitJpaRepository.save(new Commit(repo1, "sha1", "feat: 1", "Alice", "alice@corp.com", "alice", t1, 20, 5, 25, null));
        Commit c2 = commitJpaRepository.save(new Commit(repo1, "sha2", "fix: 2", "Alice", "ALICE@corp.com", "alice", t2, 10, 2, 12, null));
        Commit c3 = commitJpaRepository.save(new Commit(repo1, "sha3", "refactor: 3", "Alice", "alice@corp.com", "alice", t3, 15, 8, 23, null));

        fileChangeJpaRepository.save(new FileChange(c1, "src/Service.java", FileChangeStatus.ADDED, 20, 5, 25, null, null));
        fileChangeJpaRepository.save(new FileChange(c2, "src/Service.java", FileChangeStatus.MODIFIED, 10, 2, 12, null, null));
        fileChangeJpaRepository.save(new FileChange(c3, "src/Service.java", FileChangeStatus.MODIFIED, 15, 8, 23, null, null));
        fileChangeJpaRepository.flush();

        aggregationService.aggregateRepositoryContributorFiles(repo1.getId());

        RepositoryContributorFile record = repositoryContributorFileJpaRepository
                .findByRepositoryIdAndContributorIdAndFilePath(repo1.getId(), alice.getId(), "src/Service.java")
                .orElseThrow();

        assertThat(record.getTotalRevisions()).isEqualTo(3);
        assertThat(record.getTotalAdditions()).isEqualTo(45);
        assertThat(record.getTotalDeletions()).isEqualTo(15);
        assertThat(record.getTotalChurn()).isEqualTo(60);
        assertThat(record.getFirstContributedAt()).isEqualTo(t1);
        assertThat(record.getLastContributedAt()).isEqualTo(t3);
    }

    @Test
    @DisplayName("3. Multiple contributors on the same file produce separate rows")
    void multipleContributorsSameFile() {
        Instant t1 = Instant.parse("2026-09-01T10:00:00Z");
        Instant t2 = Instant.parse("2026-09-02T10:00:00Z");

        Commit c1 = commitJpaRepository.save(new Commit(repo1, "sha1", "Alice work", "Alice", "alice@corp.com", "alice", t1, 10, 0, 10, null));
        Commit c2 = commitJpaRepository.save(new Commit(repo1, "sha2", "Bob work", "Bob", "bob@corp.com", "bob", t2, 25, 5, 30, null));

        fileChangeJpaRepository.save(new FileChange(c1, "src/Shared.java", FileChangeStatus.ADDED, 10, 0, 10, null, null));
        fileChangeJpaRepository.save(new FileChange(c2, "src/Shared.java", FileChangeStatus.MODIFIED, 25, 5, 30, null, null));
        fileChangeJpaRepository.flush();

        aggregationService.aggregateRepositoryContributorFiles(repo1.getId());

        List<RepositoryContributorFile> records = repositoryContributorFileJpaRepository.findByRepositoryIdAndFilePath(repo1.getId(), "src/Shared.java");
        assertThat(records).hasSize(2);

        RepositoryContributorFile aliceRec = records.stream().filter(r -> r.getContributor().getId().equals(alice.getId())).findFirst().orElseThrow();
        assertThat(aliceRec.getTotalChurn()).isEqualTo(10);

        RepositoryContributorFile bobRec = records.stream().filter(r -> r.getContributor().getId().equals(bob.getId())).findFirst().orElseThrow();
        assertThat(bobRec.getTotalChurn()).isEqualTo(30);
    }

    @Test
    @DisplayName("4. One contributor on multiple files produces separate rows")
    void singleContributorMultipleFiles() {
        Instant now = Instant.now();
        Commit c = commitJpaRepository.save(new Commit(repo1, "sha1", "Multi file commit", "Alice", "alice@corp.com", "alice", now, 30, 0, 30, null));

        fileChangeJpaRepository.save(new FileChange(c, "src/FileA.java", FileChangeStatus.ADDED, 10, 0, 10, null, null));
        fileChangeJpaRepository.save(new FileChange(c, "src/FileB.java", FileChangeStatus.ADDED, 20, 0, 20, null, null));
        fileChangeJpaRepository.flush();

        aggregationService.aggregateRepositoryContributorFiles(repo1.getId());

        List<RepositoryContributorFile> records = repositoryContributorFileJpaRepository.findByRepositoryIdAndContributorId(repo1.getId(), alice.getId());
        assertThat(records).hasSize(2);
        assertThat(records).extracting(RepositoryContributorFile::getFilePath).containsExactlyInAnyOrder("src/FileA.java", "src/FileB.java");
    }

    @Test
    @DisplayName("7. Repository isolation: commits on other repositories are excluded")
    void repositoryIsolation() {
        Instant now = Instant.now();
        Commit c1 = commitJpaRepository.save(new Commit(repo1, "sha1", "Repo 1 commit", "Alice", "alice@corp.com", "alice", now, 10, 0, 10, null));
        Commit c2 = commitJpaRepository.save(new Commit(repo2, "sha2", "Repo 2 commit", "Alice", "alice@corp.com", "alice", now, 50, 0, 50, null));

        fileChangeJpaRepository.save(new FileChange(c1, "pom.xml", FileChangeStatus.MODIFIED, 10, 0, 10, null, null));
        fileChangeJpaRepository.save(new FileChange(c2, "pom.xml", FileChangeStatus.MODIFIED, 50, 0, 50, null, null));
        fileChangeJpaRepository.flush();

        aggregationService.aggregateRepositoryContributorFiles(repo1.getId());
        aggregationService.aggregateRepositoryContributorFiles(repo2.getId());

        RepositoryContributorFile r1 = repositoryContributorFileJpaRepository
                .findByRepositoryIdAndContributorIdAndFilePath(repo1.getId(), alice.getId(), "pom.xml").orElseThrow();
        RepositoryContributorFile r2 = repositoryContributorFileJpaRepository
                .findByRepositoryIdAndContributorIdAndFilePath(repo2.getId(), alice.getId(), "pom.xml").orElseThrow();

        assertThat(r1.getTotalChurn()).isEqualTo(10);
        assertThat(r2.getTotalChurn()).isEqualTo(50);
    }

    @Test
    @DisplayName("8. Null or unknown author email does not generate contributor-file records")
    void nullOrUnknownAuthorEmail() {
        Instant now = Instant.now();
        Commit cNullEmail = commitJpaRepository.save(new Commit(repo1, "sha_null", "No author", null, null, null, now, 10, 0, 10, null));
        Commit cUnknown = commitJpaRepository.save(new Commit(repo1, "sha_unk", "Unknown", "Unknown", "unknown@nowhere.com", "unk", now, 15, 0, 15, null));

        fileChangeJpaRepository.save(new FileChange(cNullEmail, "src/App.java", FileChangeStatus.ADDED, 10, 0, 10, null, null));
        fileChangeJpaRepository.save(new FileChange(cUnknown, "src/App.java", FileChangeStatus.MODIFIED, 15, 0, 15, null, null));
        fileChangeJpaRepository.flush();

        RepositoryContributorFileAggregationResult result = aggregationService.aggregateRepositoryContributorFiles(repo1.getId());
        assertThat(result.totalRowsProcessed()).isEqualTo(0);
        assertThat(repositoryContributorFileJpaRepository.findByRepositoryId(repo1.getId())).isEmpty();
    }

    @Test
    @DisplayName("9. Re-running unchanged aggregation reports unchanged rows and does NOT perform updates")
    void idempotentRerunReportsUnchanged() {
        Instant now = Instant.now();
        Commit c = commitJpaRepository.save(new Commit(repo1, "sha1", "Initial", "Alice", "alice@corp.com", "alice", now, 10, 2, 12, null));
        fileChangeJpaRepository.save(new FileChange(c, "src/App.java", FileChangeStatus.ADDED, 10, 2, 12, null, null));
        fileChangeJpaRepository.flush();

        // 1st run: created
        RepositoryContributorFileAggregationResult r1 = aggregationService.aggregateRepositoryContributorFiles(repo1.getId());
        assertThat(r1.createdCount()).isEqualTo(1);
        assertThat(r1.updatedCount()).isEqualTo(0);
        assertThat(r1.unchangedCount()).isEqualTo(0);

        entityManager.flush();
        entityManager.clear();

        RepositoryContributorFile saved = repositoryContributorFileJpaRepository
                .findByRepositoryIdAndContributorIdAndFilePath(repo1.getId(), alice.getId(), "src/App.java")
                .orElseThrow();
        Instant originalUpdatedAt = saved.getUpdatedAt();

        // 2nd run: unchanged
        RepositoryContributorFileAggregationResult r2 = aggregationService.aggregateRepositoryContributorFiles(repo1.getId());
        assertThat(r2.createdCount()).isEqualTo(0);
        assertThat(r2.updatedCount()).isEqualTo(0);
        assertThat(r2.unchangedCount()).isEqualTo(1);
        assertThat(r2.deletedCount()).isEqualTo(0);

        entityManager.flush();
        entityManager.clear();

        RepositoryContributorFile reloaded = repositoryContributorFileJpaRepository
                .findByRepositoryIdAndContributorIdAndFilePath(repo1.getId(), alice.getId(), "src/App.java")
                .orElseThrow();
        assertThat(reloaded.getUpdatedAt()).isEqualTo(originalUpdatedAt);
    }

    @Test
    @DisplayName("10 & 11. Existing row is updated and new row is created when source data evolves")
    void incrementalEvolutionUpdateAndCreate() {
        Instant t1 = Instant.parse("2026-09-01T10:00:00Z");
        Commit c1 = commitJpaRepository.save(new Commit(repo1, "sha1", "Initial", "Alice", "alice@corp.com", "alice", t1, 10, 0, 10, null));
        fileChangeJpaRepository.save(new FileChange(c1, "src/App.java", FileChangeStatus.ADDED, 10, 0, 10, null, null));
        fileChangeJpaRepository.flush();

        aggregationService.aggregateRepositoryContributorFiles(repo1.getId());

        // Add second commit updating App.java by Alice and adding Util.java by Bob
        Instant t2 = Instant.parse("2026-09-02T10:00:00Z");
        Commit c2 = commitJpaRepository.save(new Commit(repo1, "sha2", "Alice update", "Alice", "alice@corp.com", "alice", t2, 5, 2, 7, null));
        Commit c3 = commitJpaRepository.save(new Commit(repo1, "sha3", "Bob new file", "Bob", "bob@corp.com", "bob", t2, 30, 0, 30, null));

        fileChangeJpaRepository.save(new FileChange(c2, "src/App.java", FileChangeStatus.MODIFIED, 5, 2, 7, null, null));
        fileChangeJpaRepository.save(new FileChange(c3, "src/Util.java", FileChangeStatus.ADDED, 30, 0, 30, null, null));
        fileChangeJpaRepository.flush();

        RepositoryContributorFileAggregationResult result = aggregationService.aggregateRepositoryContributorFiles(repo1.getId());
        assertThat(result.createdCount()).isEqualTo(1); // Bob on Util.java
        assertThat(result.updatedCount()).isEqualTo(1); // Alice on App.java
        assertThat(result.unchangedCount()).isEqualTo(0);
        assertThat(result.deletedCount()).isEqualTo(0);

        RepositoryContributorFile aliceApp = repositoryContributorFileJpaRepository
                .findByRepositoryIdAndContributorIdAndFilePath(repo1.getId(), alice.getId(), "src/App.java").orElseThrow();
        assertThat(aliceApp.getTotalRevisions()).isEqualTo(2);
        assertThat(aliceApp.getTotalChurn()).isEqualTo(17);
        assertThat(aliceApp.getLastContributedAt()).isEqualTo(t2);
    }

    @Test
    @DisplayName("12. Stale contributor-file relationship is removed during reconciliation")
    void staleRowCleanup() {
        // Pre-create obsolete contributor-file record
        RepositoryContributorFile obsolete = new RepositoryContributorFile(
                repo1, alice, "obsolete/Old.java", 5, 50, 10, 60, Instant.now(), Instant.now()
        );
        repositoryContributorFileJpaRepository.saveAndFlush(obsolete);

        // Valid current commit on a different file
        Commit c = commitJpaRepository.save(new Commit(repo1, "sha1", "Active", "Alice", "alice@corp.com", "alice", Instant.now(), 10, 0, 10, null));
        fileChangeJpaRepository.save(new FileChange(c, "src/New.java", FileChangeStatus.ADDED, 10, 0, 10, null, null));
        fileChangeJpaRepository.flush();

        RepositoryContributorFileAggregationResult result = aggregationService.aggregateRepositoryContributorFiles(repo1.getId());
        assertThat(result.createdCount()).isEqualTo(1);
        assertThat(result.deletedCount()).isEqualTo(1);

        List<RepositoryContributorFile> remaining = repositoryContributorFileJpaRepository.findByRepositoryId(repo1.getId());
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getFilePath()).isEqualTo("src/New.java");
    }

    @Test
    @DisplayName("14 & 15. Renamed and removed file statuses aggregate faithfully according to persisted FileChange records")
    void renameAndRemoveStatuses() {
        Instant t1 = Instant.parse("2026-09-01T10:00:00Z");
        Instant t2 = Instant.parse("2026-09-02T10:00:00Z");

        Commit c1 = commitJpaRepository.save(new Commit(repo1, "sha1", "Rename", "Alice", "alice@corp.com", "alice", t1, 0, 0, 0, null));
        Commit c2 = commitJpaRepository.save(new Commit(repo1, "sha2", "Delete", "Alice", "alice@corp.com", "alice", t2, 0, 20, 20, null));

        fileChangeJpaRepository.save(new FileChange(c1, "src/Renamed.java", FileChangeStatus.RENAMED, 0, 0, 0, null, null));
        fileChangeJpaRepository.save(new FileChange(c2, "src/Removed.java", FileChangeStatus.REMOVED, 0, 20, 20, null, null));
        fileChangeJpaRepository.flush();

        RepositoryContributorFileAggregationResult result = aggregationService.aggregateRepositoryContributorFiles(repo1.getId());
        assertThat(result.totalRowsProcessed()).isEqualTo(2);

        RepositoryContributorFile renamedRec = repositoryContributorFileJpaRepository
                .findByRepositoryIdAndContributorIdAndFilePath(repo1.getId(), alice.getId(), "src/Renamed.java").orElseThrow();
        assertThat(renamedRec.getTotalRevisions()).isEqualTo(1);
        assertThat(renamedRec.getTotalChurn()).isEqualTo(0);

        RepositoryContributorFile removedRec = repositoryContributorFileJpaRepository
                .findByRepositoryIdAndContributorIdAndFilePath(repo1.getId(), alice.getId(), "src/Removed.java").orElseThrow();
        assertThat(removedRec.getTotalRevisions()).isEqualTo(1);
        assertThat(removedRec.getTotalDeletions()).isEqualTo(20);
        assertThat(removedRec.getTotalChurn()).isEqualTo(20);
    }

    @Test
    @DisplayName("16. Empty repository aggregation purges stale records and returns 0")
    void emptyRepositoryAggregation() {
        RepositoryContributorFile obsolete = new RepositoryContributorFile(
                repo1, alice, "src/Old.java", 1, 10, 0, 10, Instant.now(), Instant.now()
        );
        repositoryContributorFileJpaRepository.saveAndFlush(obsolete);

        RepositoryContributorFileAggregationResult result = aggregationService.aggregateRepositoryContributorFiles(repo1.getId());
        assertThat(result.totalRowsProcessed()).isEqualTo(0);
        assertThat(result.createdCount()).isEqualTo(0);
        assertThat(result.updatedCount()).isEqualTo(0);
        assertThat(result.unchangedCount()).isEqualTo(0);
        assertThat(result.deletedCount()).isEqualTo(1);
        assertThat(repositoryContributorFileJpaRepository.findByRepositoryId(repo1.getId())).isEmpty();
    }

    @Test
    @DisplayName("17. Non-existent repository throws ResourceNotFoundException")
    void nonExistentRepositoryThrows() {
        assertThatThrownBy(() -> aggregationService.aggregateRepositoryContributorFiles(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Repository not found with id: 999");
    }

    @Test
    @DisplayName("18. Multi-item batch aggregation processes all rows accurately without duplicate keys")
    void multiItemBatchAggregation() {
        Instant now = Instant.now();
        Commit commit = commitJpaRepository.save(new Commit(repo1, "sha_batch", "Batch commit", "Alice", "alice@corp.com", "alice", now, 100, 10, 110, null));

        for (int i = 0; i < 25; i++) {
            fileChangeJpaRepository.save(new FileChange(commit, "src/File_" + i + ".java", FileChangeStatus.ADDED, 4, 1, 5, null, null));
        }
        fileChangeJpaRepository.flush();

        RepositoryContributorFileAggregationResult result = aggregationService.aggregateRepositoryContributorFiles(repo1.getId());
        assertThat(result.totalRowsProcessed()).isEqualTo(25);
        assertThat(result.createdCount()).isEqualTo(25);
        assertThat(result.unchangedCount()).isEqualTo(0);
        assertThat(repositoryContributorFileJpaRepository.countByRepositoryId(repo1.getId())).isEqualTo(25);
    }

    @Test
    @DisplayName("19. Bounded reconciliation correctly handles mixed unchanged, updated, created, and stale rows")
    void boundedReconciliationMixedStatuses() {
        Instant t1 = Instant.parse("2026-09-01T10:00:00Z");
        Instant t2 = Instant.parse("2026-09-02T10:00:00Z");

        // 1. Pre-populate existing state: Unchanged row, Updated row, and Stale row
        RepositoryContributorFile unchangedRcf = repositoryContributorFileJpaRepository.saveAndFlush(
                new RepositoryContributorFile(repo1, alice, "src/Unchanged.java", 1, 10, 0, 10, t1, t1)
        );
        RepositoryContributorFile updatedRcf = repositoryContributorFileJpaRepository.saveAndFlush(
                new RepositoryContributorFile(repo1, alice, "src/Updated.java", 1, 5, 0, 5, t1, t1)
        );
        RepositoryContributorFile staleRcf = repositoryContributorFileJpaRepository.saveAndFlush(
                new RepositoryContributorFile(repo1, bob, "src/Stale.java", 1, 20, 0, 20, t1, t1)
        );

        entityManager.flush();
        entityManager.clear();

        RepositoryContributorFile reloadedUnchanged = repositoryContributorFileJpaRepository
                .findByRepositoryIdAndContributorIdAndFilePath(repo1.getId(), alice.getId(), "src/Unchanged.java")
                .orElseThrow();
        Instant originalUnchangedUpdatedAt = reloadedUnchanged.getUpdatedAt();

        // 2. Persist source data for Unchanged, Updated (with new second commit), and New
        Commit c1 = commitJpaRepository.save(new Commit(repo1, "sha1", "Commit 1", "Alice", "alice@corp.com", "alice", t1, 15, 0, 15, null));
        Commit c2 = commitJpaRepository.save(new Commit(repo1, "sha2", "Commit 2", "Alice", "alice@corp.com", "alice", t2, 10, 2, 12, null));
        Commit c3 = commitJpaRepository.save(new Commit(repo1, "sha3", "Commit 3", "Bob", "bob@corp.com", "bob", t2, 30, 5, 35, null));

        fileChangeJpaRepository.save(new FileChange(c1, "src/Unchanged.java", FileChangeStatus.ADDED, 10, 0, 10, null, null));
        fileChangeJpaRepository.save(new FileChange(c1, "src/Updated.java", FileChangeStatus.ADDED, 5, 0, 5, null, null));
        fileChangeJpaRepository.save(new FileChange(c2, "src/Updated.java", FileChangeStatus.MODIFIED, 10, 2, 12, null, null));
        fileChangeJpaRepository.save(new FileChange(c3, "src/New.java", FileChangeStatus.ADDED, 30, 5, 35, null, null));
        fileChangeJpaRepository.flush();

        // 3. Execute bounded aggregation
        RepositoryContributorFileAggregationResult result = aggregationService.aggregateRepositoryContributorFiles(repo1.getId());

        assertThat(result.totalRowsProcessed()).isEqualTo(3);
        assertThat(result.createdCount()).isEqualTo(1);   // Bob on src/New.java
        assertThat(result.updatedCount()).isEqualTo(1);   // Alice on src/Updated.java
        assertThat(result.unchangedCount()).isEqualTo(1); // Alice on src/Unchanged.java
        assertThat(result.deletedCount()).isEqualTo(1);   // Bob on src/Stale.java purged

        entityManager.flush();
        entityManager.clear();

        // Stale row must be purged
        assertThat(repositoryContributorFileJpaRepository.findByRepositoryIdAndContributorIdAndFilePath(repo1.getId(), bob.getId(), "src/Stale.java"))
                .isEmpty();

        // Unchanged row must be untouched
        RepositoryContributorFile finalUnchanged = repositoryContributorFileJpaRepository
                .findByRepositoryIdAndContributorIdAndFilePath(repo1.getId(), alice.getId(), "src/Unchanged.java")
                .orElseThrow();
        assertThat(finalUnchanged.getUpdatedAt()).isEqualTo(originalUnchangedUpdatedAt);

        // Updated row must reflect new aggregates
        RepositoryContributorFile finalUpdated = repositoryContributorFileJpaRepository
                .findByRepositoryIdAndContributorIdAndFilePath(repo1.getId(), alice.getId(), "src/Updated.java")
                .orElseThrow();
        assertThat(finalUpdated.getTotalRevisions()).isEqualTo(2);
        assertThat(finalUpdated.getTotalChurn()).isEqualTo(17);
        assertThat(finalUpdated.getLastContributedAt()).isEqualTo(t2);

        // New row must exist
        RepositoryContributorFile finalNew = repositoryContributorFileJpaRepository
                .findByRepositoryIdAndContributorIdAndFilePath(repo1.getId(), bob.getId(), "src/New.java")
                .orElseThrow();
        assertThat(finalNew.getTotalRevisions()).isEqualTo(1);
        assertThat(finalNew.getTotalChurn()).isEqualTo(35);
    }

    @Test
    @DisplayName("20. Exact-key lookup matches exact (contributor, file) pairs and avoids cross-product false positives")
    void exactKeyLookupAvoidsCrossProductMatches() {
        Instant now = Instant.now();

        // Database has existing materialized row for (Alice, File1) and (Bob, File1)
        repositoryContributorFileJpaRepository.saveAndFlush(
                new RepositoryContributorFile(repo1, alice, "src/File1.java", 1, 10, 0, 10, now, now)
        );
        repositoryContributorFileJpaRepository.saveAndFlush(
                new RepositoryContributorFile(repo1, bob, "src/File1.java", 1, 20, 0, 20, now, now)
        );
        entityManager.flush();
        entityManager.clear();

        // Suppose current page has keys: (Alice, File1) and (Bob, File2)
        // With IN/IN query: contributor_id IN (Alice, Bob) AND file_path IN (File1, File2)
        // it would return (Bob, File1) as a cross-product false positive.
        // With exact composite key query:
        List<String> pageKeys = List.of(
                alice.getId() + ":::src/File1.java",
                bob.getId() + ":::src/File2.java"
        );

        List<RepositoryContributorFile> matched = repositoryContributorFileJpaRepository.findByRepositoryIdAndCompositeKeys(
                repo1.getId(), pageKeys
        );

        // Must ONLY match (Alice, File1) and NEVER match (Bob, File1)
        assertThat(matched).hasSize(1);
        assertThat(matched.get(0).getContributor().getId()).isEqualTo(alice.getId());
        assertThat(matched.get(0).getFilePath()).isEqualTo("src/File1.java");
    }
}
