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
}
