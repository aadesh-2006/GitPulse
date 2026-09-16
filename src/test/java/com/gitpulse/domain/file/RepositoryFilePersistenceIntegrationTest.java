package com.gitpulse.domain.file;

import com.gitpulse.domain.commit.Commit;
import com.gitpulse.domain.commit.CommitJpaRepository;
import com.gitpulse.domain.contributor.Contributor;
import com.gitpulse.domain.contributor.ContributorJpaRepository;
import com.gitpulse.domain.file.dto.FilePrimaryContributorRow;
import com.gitpulse.domain.file.dto.RepositoryFileAggregationResult;
import com.gitpulse.domain.file.dto.RepositoryFileAggregationRow;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import(RepositoryFileAggregationService.class)
class RepositoryFilePersistenceIntegrationTest {

    @Autowired
    private RepositoryFileJpaRepository repositoryFileJpaRepository;

    @Autowired
    private RepositoryJpaRepository repositoryJpaRepository;

    @Autowired
    private CommitJpaRepository commitJpaRepository;

    @Autowired
    private FileChangeJpaRepository fileChangeJpaRepository;

    @Autowired
    private ContributorJpaRepository contributorJpaRepository;

    @Autowired
    private RepositoryFileAggregationService repositoryFileAggregationService;

    @Autowired
    private EntityManager entityManager;

    private Repository repo1;
    private Repository repo2;

    @BeforeEach
    void setUp() {
        repo1 = repositoryJpaRepository.save(new Repository("owner1", "repo1", "Repo 1", "main"));
        repo2 = repositoryJpaRepository.save(new Repository("owner2", "repo2", "Repo 2", "main"));
    }

    @Test
    @DisplayName("Should persist RepositoryFile with valid Repository and verify fields and timestamps")
    void persistRepositoryFile() {
        Instant t1 = Instant.parse("2026-09-01T10:00:00Z");
        Instant t2 = Instant.parse("2026-09-05T12:00:00Z");

        RepositoryFile rf = new RepositoryFile(
                repo1,
                "src/main/java/com/gitpulse/App.java",
                "App.java",
                "java",
                "src/main/java/com/gitpulse",
                10,
                150,
                20,
                170,
                false,
                t1,
                t2,
                null
        );

        RepositoryFile saved = repositoryFileJpaRepository.saveAndFlush(rf);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getFilePath()).isEqualTo("src/main/java/com/gitpulse/App.java");
        assertThat(saved.getFileName()).isEqualTo("App.java");
        assertThat(saved.getExtension()).isEqualTo("java");
        assertThat(saved.getDirectoryPath()).isEqualTo("src/main/java/com/gitpulse");
        assertThat(saved.getTotalRevisions()).isEqualTo(10);
        assertThat(saved.getTotalAdditions()).isEqualTo(150);
        assertThat(saved.getTotalDeletions()).isEqualTo(20);
        assertThat(saved.getTotalChurn()).isEqualTo(170);
        assertThat(saved.isDeleted()).isFalse();
        assertThat(saved.getFirstModifiedAt()).isEqualTo(t1);
        assertThat(saved.getLastModifiedAt()).isEqualTo(t2);
        assertThat(saved.getPrimaryContributor()).isNull();
    }

    @Test
    @DisplayName("Should persist RepositoryFile with primary contributor reference")
    void persistWithPrimaryContributor() {
        Contributor contributor = contributorJpaRepository.saveAndFlush(
                new Contributor("dev@gitpulse.com", "dev", "Dev User")
        );

        Instant now = Instant.now();
        RepositoryFile rf = new RepositoryFile(
                repo1,
                "README.md",
                "README.md",
                "md",
                "",
                3,
                50,
                5,
                55,
                false,
                now,
                now,
                contributor
        );

        RepositoryFile saved = repositoryFileJpaRepository.saveAndFlush(rf);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getPrimaryContributor()).isNotNull();
        assertThat(saved.getPrimaryContributor().getId()).isEqualTo(contributor.getId());
        assertThat(saved.getPrimaryContributor().getEmail()).isEqualTo("dev@gitpulse.com");
    }

    @Test
    @DisplayName("Should enforce UNIQUE(repository_id, file_path) constraint")
    void enforceUniqueRepositoryFilePathConstraint() {
        Instant now = Instant.now();
        RepositoryFile rf1 = new RepositoryFile(
                repo1, "src/config.json", "config.json", "json", "src",
                1, 10, 0, 10, false, now, now, null
        );
        repositoryFileJpaRepository.saveAndFlush(rf1);

        RepositoryFile rf2 = new RepositoryFile(
                repo1, "src/config.json", "config.json", "json", "src",
                2, 5, 2, 7, false, now, now, null
        );

        assertThatThrownBy(() -> repositoryFileJpaRepository.saveAndFlush(rf2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should allow same file_path in different repositories")
    void allowSameFilePathInDifferentRepositories() {
        Instant now = Instant.now();
        RepositoryFile rf1 = new RepositoryFile(
                repo1, "pom.xml", "pom.xml", "xml", "",
                5, 100, 10, 110, false, now, now, null
        );
        RepositoryFile rf2 = new RepositoryFile(
                repo2, "pom.xml", "pom.xml", "xml", "",
                8, 200, 30, 230, false, now, now, null
        );

        RepositoryFile saved1 = repositoryFileJpaRepository.saveAndFlush(rf1);
        RepositoryFile saved2 = repositoryFileJpaRepository.saveAndFlush(rf2);

        assertThat(saved1.getId()).isNotNull();
        assertThat(saved2.getId()).isNotNull();
        assertThat(saved1.getId()).isNotEqualTo(saved2.getId());
        assertThat(saved1.getFilePath()).isEqualTo(saved2.getFilePath());
    }

    @Test
    @DisplayName("Should cascade delete repository_files when repository is deleted")
    void cascadeDeleteOnRepositoryRemoval() {
        Instant now = Instant.now();
        repositoryFileJpaRepository.saveAndFlush(new RepositoryFile(
                repo1, "file1.txt", "file1.txt", "txt", "",
                1, 5, 0, 5, false, now, now, null
        ));
        repositoryFileJpaRepository.saveAndFlush(new RepositoryFile(
                repo1, "file2.txt", "file2.txt", "txt", "",
                2, 10, 2, 12, false, now, now, null
        ));

        assertThat(repositoryFileJpaRepository.findByRepositoryId(repo1.getId())).hasSize(2);

        repositoryJpaRepository.delete(repo1);
        repositoryJpaRepository.flush();

        assertThat(repositoryFileJpaRepository.findByRepositoryId(repo1.getId())).isEmpty();
    }

    @Test
    @DisplayName("Deleting contributor sets primary_contributor_id to NULL via ON DELETE SET NULL")
    void setNullOnContributorDeletion() {
        Contributor contributor = contributorJpaRepository.saveAndFlush(
                new Contributor("lead@gitpulse.com", "lead", "Lead Dev")
        );

        Instant now = Instant.now();
        RepositoryFile rf = repositoryFileJpaRepository.saveAndFlush(new RepositoryFile(
                repo1, "service.java", "service.java", "java", "",
                1, 20, 5, 25, false, now, now, contributor
        ));

        Long fileId = rf.getId();
        assertThat(rf.getPrimaryContributor()).isNotNull();

        entityManager.flush();
        entityManager.clear();

        contributorJpaRepository.deleteById(contributor.getId());
        entityManager.flush();
        entityManager.clear();

        Optional<RepositoryFile> reloaded = repositoryFileJpaRepository.findById(fileId);
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getPrimaryContributor()).isNull();
    }

    @Test
    @DisplayName("A & B. Native SQL aggregation calculates accurate revisions, additions, deletions, churn, and timestamp bounds")
    void nativeAggregationAndMetricCalculation() {
        Instant t1 = Instant.parse("2026-09-01T10:00:00Z");
        Instant t2 = Instant.parse("2026-09-02T10:00:00Z");
        Instant t3 = Instant.parse("2026-09-03T10:00:00Z");

        Commit c1 = commitJpaRepository.save(new Commit(repo1, "sha1", "msg1", "Alice", "alice@corp.com", "alice", t1, 10, 2, 12, null));
        Commit c2 = commitJpaRepository.save(new Commit(repo1, "sha2", "msg2", "Alice", "alice@corp.com", "alice", t2, 20, 5, 25, null));
        Commit c3 = commitJpaRepository.save(new Commit(repo1, "sha3", "msg3", "Bob", "bob@corp.com", "bob", t3, 15, 3, 18, null));

        fileChangeJpaRepository.save(new FileChange(c1, "src/App.java", FileChangeStatus.ADDED, 10, 2, 12, null, null));
        fileChangeJpaRepository.save(new FileChange(c2, "src/App.java", FileChangeStatus.MODIFIED, 20, 5, 25, null, null));
        fileChangeJpaRepository.save(new FileChange(c3, "src/App.java", FileChangeStatus.MODIFIED, 15, 3, 18, null, null));
        fileChangeJpaRepository.save(new FileChange(c1, "README.md", FileChangeStatus.ADDED, 5, 0, 5, null, null));
        fileChangeJpaRepository.flush();

        List<RepositoryFileAggregationRow> rows = repositoryFileJpaRepository.aggregateFilesByRepositoryId(repo1.getId());

        assertThat(rows).hasSize(2);

        RepositoryFileAggregationRow appRow = rows.stream()
                .filter(r -> r.getFilePath().equals("src/App.java"))
                .findFirst()
                .orElseThrow();

        assertThat(appRow.getTotalRevisions()).isEqualTo(3);
        assertThat(appRow.getTotalAdditions()).isEqualTo(45);
        assertThat(appRow.getTotalDeletions()).isEqualTo(10);
        assertThat(appRow.getTotalChurn()).isEqualTo(55);
        assertThat(appRow.getFirstModifiedAtInstant()).isEqualTo(t1);
        assertThat(appRow.getLastModifiedAtInstant()).isEqualTo(t3);
        assertThat(appRow.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("C. Latest-status deletion logic: ADDED -> MODIFIED -> REMOVED results in is_deleted = true")
    void deletionLogicLatestStatusRemoved() {
        Instant t1 = Instant.parse("2026-09-01T10:00:00Z");
        Instant t2 = Instant.parse("2026-09-02T10:00:00Z");
        Instant t3 = Instant.parse("2026-09-03T10:00:00Z");

        Commit c1 = commitJpaRepository.save(new Commit(repo1, "sha1", "msg1", "Alice", "alice@corp.com", "alice", t1, 10, 0, 10, null));
        Commit c2 = commitJpaRepository.save(new Commit(repo1, "sha2", "msg2", "Alice", "alice@corp.com", "alice", t2, 5, 2, 7, null));
        Commit c3 = commitJpaRepository.save(new Commit(repo1, "sha3", "msg3", "Alice", "alice@corp.com", "alice", t3, 0, 15, 15, null));

        fileChangeJpaRepository.save(new FileChange(c1, "deleted_file.txt", FileChangeStatus.ADDED, 10, 0, 10, null, null));
        fileChangeJpaRepository.save(new FileChange(c2, "deleted_file.txt", FileChangeStatus.MODIFIED, 5, 2, 7, null, null));
        fileChangeJpaRepository.save(new FileChange(c3, "deleted_file.txt", FileChangeStatus.REMOVED, 0, 15, 15, null, null));
        fileChangeJpaRepository.flush();

        List<RepositoryFileAggregationRow> rows = repositoryFileJpaRepository.aggregateFilesByRepositoryId(repo1.getId());
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).isDeleted()).isTrue();
    }

    @Test
    @DisplayName("C. Latest-status deletion logic reverse: ADDED -> REMOVED -> MODIFIED results in is_deleted = false")
    void deletionLogicLatestStatusRecreated() {
        Instant t1 = Instant.parse("2026-09-01T10:00:00Z");
        Instant t2 = Instant.parse("2026-09-02T10:00:00Z");
        Instant t3 = Instant.parse("2026-09-03T10:00:00Z");

        Commit c1 = commitJpaRepository.save(new Commit(repo1, "sha1", "msg1", "Alice", "alice@corp.com", "alice", t1, 10, 0, 10, null));
        Commit c2 = commitJpaRepository.save(new Commit(repo1, "sha2", "msg2", "Alice", "alice@corp.com", "alice", t2, 0, 10, 10, null));
        Commit c3 = commitJpaRepository.save(new Commit(repo1, "sha3", "msg3", "Alice", "alice@corp.com", "alice", t3, 20, 0, 20, null));

        fileChangeJpaRepository.save(new FileChange(c1, "resurrected.txt", FileChangeStatus.ADDED, 10, 0, 10, null, null));
        fileChangeJpaRepository.save(new FileChange(c2, "resurrected.txt", FileChangeStatus.REMOVED, 0, 10, 10, null, null));
        fileChangeJpaRepository.save(new FileChange(c3, "resurrected.txt", FileChangeStatus.MODIFIED, 20, 0, 20, null, null));
        fileChangeJpaRepository.flush();

        List<RepositoryFileAggregationRow> rows = repositoryFileJpaRepository.aggregateFilesByRepositoryId(repo1.getId());
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).isDeleted()).isFalse();
    }

    @Test
    @DisplayName("D. Primary contributor is chosen by highest contribution count")
    void primaryContributorByHighestCount() {
        Contributor alice = contributorJpaRepository.save(new Contributor("alice@corp.com", "alice", "Alice"));
        Contributor bob = contributorJpaRepository.save(new Contributor("bob@corp.com", "bob", "Bob"));

        Instant t1 = Instant.parse("2026-09-01T10:00:00Z");
        Instant t2 = Instant.parse("2026-09-02T10:00:00Z");
        Instant t3 = Instant.parse("2026-09-03T10:00:00Z");

        Commit c1 = commitJpaRepository.save(new Commit(repo1, "sha1", "msg1", "Alice", "alice@corp.com", "alice", t1, 10, 0, 10, null));
        Commit c2 = commitJpaRepository.save(new Commit(repo1, "sha2", "msg2", "Alice", "alice@corp.com", "alice", t2, 5, 0, 5, null));
        Commit c3 = commitJpaRepository.save(new Commit(repo1, "sha3", "msg3", "Bob", "bob@corp.com", "bob", t3, 20, 0, 20, null));

        fileChangeJpaRepository.save(new FileChange(c1, "App.java", FileChangeStatus.ADDED, 10, 0, 10, null, null));
        fileChangeJpaRepository.save(new FileChange(c2, "App.java", FileChangeStatus.MODIFIED, 5, 0, 5, null, null));
        fileChangeJpaRepository.save(new FileChange(c3, "App.java", FileChangeStatus.MODIFIED, 20, 0, 20, null, null));
        fileChangeJpaRepository.flush();

        List<FilePrimaryContributorRow> contribRows = repositoryFileJpaRepository.findPrimaryContributorsByRepositoryId(repo1.getId());
        assertThat(contribRows).isNotEmpty();

        FilePrimaryContributorRow topRow = contribRows.get(0);
        assertThat(topRow.getFilePath()).isEqualTo("App.java");
        assertThat(topRow.getContributorId()).isEqualTo(alice.getId());
        assertThat(topRow.getContributionCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("E. Primary contributor tie resolves deterministically using latest contribution timestamp")
    void primaryContributorTieResolvedByTimestamp() {
        Contributor alice = contributorJpaRepository.save(new Contributor("alice@corp.com", "alice", "Alice"));
        Contributor bob = contributorJpaRepository.save(new Contributor("bob@corp.com", "bob", "Bob"));

        Instant t1 = Instant.parse("2026-09-01T10:00:00Z");
        Instant t2 = Instant.parse("2026-09-02T10:00:00Z"); // Bob commits later

        Commit c1 = commitJpaRepository.save(new Commit(repo1, "sha1", "msg1", "Alice", "alice@corp.com", "alice", t1, 10, 0, 10, null));
        Commit c2 = commitJpaRepository.save(new Commit(repo1, "sha2", "msg2", "Bob", "bob@corp.com", "bob", t2, 10, 0, 10, null));

        fileChangeJpaRepository.save(new FileChange(c1, "Shared.java", FileChangeStatus.ADDED, 10, 0, 10, null, null));
        fileChangeJpaRepository.save(new FileChange(c2, "Shared.java", FileChangeStatus.MODIFIED, 10, 0, 10, null, null));
        fileChangeJpaRepository.flush();

        List<FilePrimaryContributorRow> contribRows = repositoryFileJpaRepository.findPrimaryContributorsByRepositoryId(repo1.getId());
        assertThat(contribRows).hasSize(2);

        // Bob has later commit timestamp -> Bob is ordered first
        FilePrimaryContributorRow topRow = contribRows.get(0);
        assertThat(topRow.getFilePath()).isEqualTo("Shared.java");
        assertThat(topRow.getContributorId()).isEqualTo(bob.getId());
        assertThat(topRow.getContributionCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("F. Idempotent rerun: running aggregation twice does not double counts")
    void idempotentRerun() {
        contributorJpaRepository.save(new Contributor("alice@corp.com", "alice", "Alice"));

        Instant t1 = Instant.parse("2026-09-01T10:00:00Z");
        Commit c1 = commitJpaRepository.save(new Commit(repo1, "sha1", "msg1", "Alice", "alice@corp.com", "alice", t1, 10, 2, 12, null));
        fileChangeJpaRepository.save(new FileChange(c1, "Idempotent.java", FileChangeStatus.ADDED, 10, 2, 12, null, null));
        fileChangeJpaRepository.flush();

        RepositoryFileAggregationResult r1 = repositoryFileAggregationService.aggregateRepositoryFiles(repo1.getId());
        assertThat(r1.createdCount()).isEqualTo(1);
        assertThat(r1.updatedCount()).isEqualTo(0);

        RepositoryFile file1 = repositoryFileJpaRepository.findByRepositoryIdAndFilePath(repo1.getId(), "Idempotent.java").orElseThrow();
        assertThat(file1.getTotalRevisions()).isEqualTo(1);
        assertThat(file1.getTotalChurn()).isEqualTo(12);

        // Second run
        RepositoryFileAggregationResult r2 = repositoryFileAggregationService.aggregateRepositoryFiles(repo1.getId());
        assertThat(r2.createdCount()).isEqualTo(0);
        assertThat(r2.updatedCount()).isEqualTo(1);

        RepositoryFile file2 = repositoryFileJpaRepository.findByRepositoryIdAndFilePath(repo1.getId(), "Idempotent.java").orElseThrow();
        assertThat(file2.getTotalRevisions()).isEqualTo(1);
        assertThat(file2.getTotalChurn()).isEqualTo(12);
        assertThat(repositoryFileJpaRepository.findByRepositoryId(repo1.getId())).hasSize(1);
    }

    @Test
    @DisplayName("G. Stale row cleanup: removes existing repository_files that no longer exist in source aggregation")
    void staleRowCleanup() {
        // Pre-create an obsolete repository file
        RepositoryFile obsoleteFile = new RepositoryFile(
                repo1, "old/obsolete.java", "obsolete.java", "java", "old",
                5, 50, 10, 60, false, Instant.now(), Instant.now(), null
        );
        repositoryFileJpaRepository.saveAndFlush(obsoleteFile);

        // Add a real commit for a different file
        Instant t1 = Instant.parse("2026-09-01T10:00:00Z");
        Commit c1 = commitJpaRepository.save(new Commit(repo1, "sha1", "msg1", "Alice", "alice@corp.com", "alice", t1, 10, 0, 10, null));
        fileChangeJpaRepository.save(new FileChange(c1, "new/active.java", FileChangeStatus.ADDED, 10, 0, 10, null, null));
        fileChangeJpaRepository.flush();

        RepositoryFileAggregationResult result = repositoryFileAggregationService.aggregateRepositoryFiles(repo1.getId());
        assertThat(result.createdCount()).isEqualTo(1);
        assertThat(result.deletedCount()).isEqualTo(1);

        List<RepositoryFile> currentFiles = repositoryFileJpaRepository.findByRepositoryId(repo1.getId());
        assertThat(currentFiles).hasSize(1);
        assertThat(currentFiles.get(0).getFilePath()).isEqualTo("new/active.java");
    }

    @Test
    @DisplayName("H. Multi-repository isolation: same file path in two repositories produces independent rows")
    void multiRepositoryIsolation() {
        Instant t1 = Instant.parse("2026-09-01T10:00:00Z");
        Commit c1 = commitJpaRepository.save(new Commit(repo1, "sha1", "msg1", "Alice", "alice@corp.com", "alice", t1, 10, 0, 10, null));
        Commit c2 = commitJpaRepository.save(new Commit(repo2, "sha2", "msg2", "Bob", "bob@corp.com", "bob", t1, 30, 0, 30, null));

        fileChangeJpaRepository.save(new FileChange(c1, "common/config.yaml", FileChangeStatus.ADDED, 10, 0, 10, null, null));
        fileChangeJpaRepository.save(new FileChange(c2, "common/config.yaml", FileChangeStatus.ADDED, 30, 0, 30, null, null));
        fileChangeJpaRepository.flush();

        repositoryFileAggregationService.aggregateRepositoryFiles(repo1.getId());
        repositoryFileAggregationService.aggregateRepositoryFiles(repo2.getId());

        RepositoryFile repo1File = repositoryFileJpaRepository.findByRepositoryIdAndFilePath(repo1.getId(), "common/config.yaml").orElseThrow();
        RepositoryFile repo2File = repositoryFileJpaRepository.findByRepositoryIdAndFilePath(repo2.getId(), "common/config.yaml").orElseThrow();

        assertThat(repo1File.getTotalChurn()).isEqualTo(10);
        assertThat(repo2File.getTotalChurn()).isEqualTo(30);
    }

    @Test
    @DisplayName("J. Empty repository aggregation completes safely with zero files")
    void emptyRepositoryAggregation() {
        RepositoryFileAggregationResult result = repositoryFileAggregationService.aggregateRepositoryFiles(repo1.getId());

        assertThat(result.totalFilesProcessed()).isEqualTo(0);
        assertThat(result.createdCount()).isEqualTo(0);
        assertThat(result.updatedCount()).isEqualTo(0);
        assertThat(result.deletedCount()).isEqualTo(0);
        assertThat(repositoryFileJpaRepository.findByRepositoryId(repo1.getId())).isEmpty();
    }
}