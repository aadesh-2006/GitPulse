package com.gitpulse.domain.commit;

import com.gitpulse.domain.repository.Repository;
import com.gitpulse.domain.repository.RepositoryJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class CommitPersistenceIntegrationTest {

    @Autowired
    private CommitJpaRepository commitJpaRepository;

    @Autowired
    private RepositoryJpaRepository repositoryJpaRepository;

    private Repository repo1;
    private Repository repo2;

    @BeforeEach
    void setUp() {
        repo1 = repositoryJpaRepository.save(new Repository("owner1", "repo1", "Repo 1", "main"));
        repo2 = repositoryJpaRepository.save(new Repository("owner2", "repo2", "Repo 2", "main"));
    }

    @Test
    @DisplayName("Should successfully persist and query a commit")
    void persistAndQueryCommit() {
        Commit commit = new Commit(
                repo1,
                "sha1111111111111111111111111111111111111",
                "feat: initial commit",
                "Alice",
                "alice@example.com",
                "alice",
                Instant.parse("2026-09-01T12:00:00Z"),
                10,
                2,
                12,
                "https://github.com/owner1/repo1/commit/sha1111"
        );

        Commit saved = commitJpaRepository.saveAndFlush(commit);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getGithubCommitSha()).isEqualTo("sha1111111111111111111111111111111111111");
        assertThat(commitJpaRepository.existsByRepositoryIdAndGithubCommitSha(repo1.getId(), "sha1111111111111111111111111111111111111")).isTrue();
    }

    @Test
    @DisplayName("Should reject duplicate commit with same SHA for the same repository")
    void rejectDuplicateShaSameRepository() {
        String sha = "sha_unique_test_123456789012345678901234";

        Commit commit1 = new Commit(repo1, sha, "Commit 1", "Alice", "alice@example.com", "alice", Instant.now(), null, null, null, null);
        commitJpaRepository.saveAndFlush(commit1);

        Commit commit2 = new Commit(repo1, sha, "Commit 2", "Bob", "bob@example.com", "bob", Instant.now(), null, null, null, null);

        assertThatThrownBy(() -> commitJpaRepository.saveAndFlush(commit2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should allow same commit SHA for different repositories")
    void allowSameShaDifferentRepositories() {
        String sha = "sha_shared_fork_123456789012345678901234";

        Commit commit1 = new Commit(repo1, sha, "Commit on upstream", "Alice", "alice@example.com", "alice", Instant.now(), null, null, null, null);
        Commit commit2 = new Commit(repo2, sha, "Commit on fork", "Alice", "alice@example.com", "alice", Instant.now(), null, null, null, null);

        Commit saved1 = commitJpaRepository.saveAndFlush(commit1);
        Commit saved2 = commitJpaRepository.saveAndFlush(commit2);

        assertThat(saved1.getId()).isNotNull();
        assertThat(saved2.getId()).isNotNull();
        assertThat(saved1.getId()).isNotEqualTo(saved2.getId());
    }

    @Test
    @DisplayName("findExistingGithubCommitShas should return matching SHAs in batch")
    void findExistingGithubCommitShas() {
        Commit c1 = new Commit(repo1, "sha_a", "Msg A", "A", "a@test.com", "a", Instant.now(), null, null, null, null);
        Commit c2 = new Commit(repo1, "sha_b", "Msg B", "B", "b@test.com", "b", Instant.now(), null, null, null, null);
        commitJpaRepository.saveAllAndFlush(List.of(c1, c2));

        List<String> found = commitJpaRepository.findExistingGithubCommitShas(repo1.getId(), List.of("sha_a", "sha_c", "sha_b", "sha_d"));

        assertThat(found).containsExactlyInAnyOrder("sha_a", "sha_b");
    }

    @Test
    @DisplayName("Pagination and ordering by committedAt should return commits newest first")
    void paginationOrdering() {
        Instant t1 = Instant.parse("2026-09-01T10:00:00Z");
        Instant t2 = Instant.parse("2026-09-02T10:00:00Z");
        Instant t3 = Instant.parse("2026-09-03T10:00:00Z");

        Commit c1 = new Commit(repo1, "sha_old", "Old", "A", "a@test.com", "a", t1, null, null, null, null);
        Commit c2 = new Commit(repo1, "sha_mid", "Mid", "A", "a@test.com", "a", t2, null, null, null, null);
        Commit c3 = new Commit(repo1, "sha_new", "New", "A", "a@test.com", "a", t3, null, null, null, null);
        commitJpaRepository.saveAllAndFlush(List.of(c1, c2, c3));

        Page<Commit> page = commitJpaRepository.findByRepositoryIdOrderByCommittedAtDesc(repo1.getId(), PageRequest.of(0, 2));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getGithubCommitSha()).isEqualTo("sha_new");
        assertThat(page.getContent().get(1).getGithubCommitSha()).isEqualTo("sha_mid");
    }

    @Test
    @DisplayName("Should persist and read back a commit with null classification")
    void persistCommitWithNullClassification() {
        Commit commit = new Commit(
                repo1,
                "sha_null_classification_1234567890123456",
                "docs: update readme",
                "Alice",
                "alice@example.com",
                "alice",
                Instant.now(),
                5,
                1,
                6,
                "https://github.com/owner1/repo1/commit/sha_null",
                null
        );

        Commit saved = commitJpaRepository.saveAndFlush(commit);
        Commit found = commitJpaRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getClassification()).isNull();
    }

    @Test
    @DisplayName("Should persist and read back each CommitClassification enum value correctly")
    void persistAndReadBackEachClassificationEnumValue() {
        int index = 0;
        for (CommitClassification classification : CommitClassification.values()) {
            String sha = String.format("sha_class_%02d_123456789012345678901234567", index++);
            Commit commit = new Commit(
                    repo1,
                    sha,
                    "Commit for " + classification.name(),
                    "Alice",
                    "alice@example.com",
                    "alice",
                    Instant.now(),
                    10,
                    5,
                    15,
                    "https://github.com/owner1/repo1/commit/" + sha,
                    classification
            );

            Commit saved = commitJpaRepository.saveAndFlush(commit);
            Commit found = commitJpaRepository.findById(saved.getId()).orElseThrow();

            assertThat(found.getClassification()).isEqualTo(classification);
        }
    }

    @Test
    @DisplayName("Should update classification on an existing commit")
    void updateClassification() {
        Commit commit = new Commit(
                repo1,
                "sha_to_update_12345678901234567890123456",
                "feat: new feature",
                "Alice",
                "alice@example.com",
                "alice",
                Instant.now(),
                20,
                0,
                20,
                "https://github.com/owner1/repo1/commit/sha_to_update"
        );
        Commit saved = commitJpaRepository.saveAndFlush(commit);
        assertThat(saved.getClassification()).isNull();

        saved.setClassification(CommitClassification.FEATURE);
        commitJpaRepository.saveAndFlush(saved);

        Commit updated = commitJpaRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getClassification()).isEqualTo(CommitClassification.FEATURE);
    }

    @Test
    @DisplayName("findByIdAndRepositoryId should strictly enforce repository isolation")
    void findByIdAndRepositoryId() {
        Commit commit1 = commitJpaRepository.saveAndFlush(new Commit(
                repo1, "sha_repo1_iso", "Msg 1", "A", "a@test.com", "a", Instant.now(), null, null, null, null
        ));

        assertThat(commitJpaRepository.findByIdAndRepositoryId(commit1.getId(), repo1.getId())).isPresent();
        assertThat(commitJpaRepository.findByIdAndRepositoryId(commit1.getId(), repo2.getId())).isEmpty();
    }

    @Test
    @DisplayName("findByRepositoryIdWithFilters should filter by classification, authorEmail, date range and repository")
    void findByRepositoryIdWithFilters() {
        Instant t1 = Instant.parse("2026-09-01T10:00:00Z");
        Instant t2 = Instant.parse("2026-09-02T10:00:00Z");
        Instant t3 = Instant.parse("2026-09-03T10:00:00Z");

        Commit c1 = new Commit(repo1, "sha_f1", "feat: 1", "Alice", "alice@example.com", "alice", t1, 10, 0, 10, null, CommitClassification.FEATURE);
        Commit c2 = new Commit(repo1, "sha_f2", "fix: 2", "Alice", "ALICE@example.com", "alice", t2, 5, 2, 7, null, CommitClassification.BUG_FIX);
        Commit c3 = new Commit(repo1, "sha_f3", "refactor: 3", "Bob", "bob@example.com", "bob", t3, 20, 5, 25, null, CommitClassification.REFACTOR);
        Commit cRepo2 = new Commit(repo2, "sha_f4", "feat: other repo", "Alice", "alice@example.com", "alice", t1, 10, 0, 10, null, CommitClassification.FEATURE);
        commitJpaRepository.saveAllAndFlush(List.of(c1, c2, c3, cRepo2));

        // 1. No filters (all for repo1)
        Page<Commit> allRepo1 = commitJpaRepository.findByRepositoryIdWithFilters(repo1.getId(), null, null, null, null, PageRequest.of(0, 10));
        assertThat(allRepo1.getTotalElements()).isEqualTo(3);

        // 2. Filter by classification
        Page<Commit> feats = commitJpaRepository.findByRepositoryIdWithFilters(repo1.getId(), CommitClassification.FEATURE, null, null, null, PageRequest.of(0, 10));
        assertThat(feats.getTotalElements()).isEqualTo(1);
        assertThat(feats.getContent().get(0).getGithubCommitSha()).isEqualTo("sha_f1");

        // 3. Filter by authorEmail (case-insensitive)
        Page<Commit> aliceCommits = commitJpaRepository.findByRepositoryIdWithFilters(repo1.getId(), null, "alice@example.com", null, null, PageRequest.of(0, 10));
        assertThat(aliceCommits.getTotalElements()).isEqualTo(2);

        // 4. Filter by date range (from / to)
        Page<Commit> midRange = commitJpaRepository.findByRepositoryIdWithFilters(
                repo1.getId(), null, null, Instant.parse("2026-09-01T12:00:00Z"), Instant.parse("2026-09-02T12:00:00Z"), PageRequest.of(0, 10)
        );
        assertThat(midRange.getTotalElements()).isEqualTo(1);
        assertThat(midRange.getContent().get(0).getGithubCommitSha()).isEqualTo("sha_f2");

        // 5. Combined filter
        Page<Commit> combined = commitJpaRepository.findByRepositoryIdWithFilters(
                repo1.getId(), CommitClassification.BUG_FIX, "alice@example.com", t1, t3, PageRequest.of(0, 10)
        );
        assertThat(combined.getTotalElements()).isEqualTo(1);
        assertThat(combined.getContent().get(0).getGithubCommitSha()).isEqualTo("sha_f2");
    }
}
