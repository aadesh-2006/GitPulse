package com.gitpulse.domain.contributor;

import com.gitpulse.domain.commit.Commit;
import com.gitpulse.domain.commit.CommitJpaRepository;
import com.gitpulse.domain.contributor.dto.ContributorAggregationRow;
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
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class ContributorPersistenceIntegrationTest {

    @Autowired
    private ContributorJpaRepository contributorJpaRepository;

    @Autowired
    private RepositoryContributorJpaRepository repositoryContributorJpaRepository;

    @Autowired
    private RepositoryJpaRepository repositoryJpaRepository;

    @Autowired
    private CommitJpaRepository commitJpaRepository;

    private Repository repo1;
    private Repository repo2;

    @BeforeEach
    void setUp() {
        repo1 = repositoryJpaRepository.save(new Repository("owner1", "repo1", "Repo 1", "main"));
        repo2 = repositoryJpaRepository.save(new Repository("owner2", "repo2", "Repo 2", "main"));
    }

    @Test
    @DisplayName("Should persist Contributor and enforce unique email constraint")
    void persistContributorAndEnforceEmailUniqueness() {
        Contributor c1 = new Contributor("alice@example.com", "alice", "Alice Smith");
        Contributor saved = contributorJpaRepository.saveAndFlush(c1);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("alice@example.com");

        Contributor c2 = new Contributor("alice@example.com", "alice_dup", "Alice Duplicate");
        assertThatThrownBy(() -> contributorJpaRepository.saveAndFlush(c2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should persist RepositoryContributor and enforce composite uniqueness")
    void persistRepositoryContributorAndEnforceUniqueness() {
        Contributor contributor = contributorJpaRepository.saveAndFlush(
                new Contributor("bob@example.com", "bob", "Bob Jones")
        );

        Instant t1 = Instant.parse("2026-09-01T10:00:00Z");
        Instant t2 = Instant.parse("2026-09-05T12:00:00Z");

        RepositoryContributor rc = new RepositoryContributor(
                repo1, contributor, 5, 100, 20, 120, t1, t2
        );
        RepositoryContributor saved = repositoryContributorJpaRepository.saveAndFlush(rc);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTotalCommits()).isEqualTo(5);
        assertThat(saved.getTotalChanges()).isEqualTo(120);

        RepositoryContributor duplicateRc = new RepositoryContributor(
                repo1, contributor, 2, 10, 5, 15, t1, t2
        );
        assertThatThrownBy(() -> repositoryContributorJpaRepository.saveAndFlush(duplicateRc))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should execute native SQL aggregation across commits and return deterministic representative name/username")
    void executeNativeContributorAggregation() {
        Instant t1 = Instant.parse("2026-09-01T10:00:00Z");
        Instant t2 = Instant.parse("2026-09-02T10:00:00Z");
        Instant t3 = Instant.parse("2026-09-03T10:00:00Z");

        // Alice commits (latest has updated name & username)
        Commit c1 = new Commit(repo1, "sha1", "msg1", "Alice Initial", "alice@corp.com", "alice_old", t1, 10, 2, 12, null);
        Commit c2 = new Commit(repo1, "sha2", "msg2", "Alice Current", "alice@corp.com", "alice_dev", t3, 20, 5, 25, null);

        // Bob commit
        Commit c3 = new Commit(repo1, "sha3", "msg3", "Bob Developer", "bob@corp.com", "bob_dev", t2, 5, 1, 6, null);

        // Commit on different repository (should be ignored)
        Commit c4 = new Commit(repo2, "sha4", "msg4", "Charlie", "charlie@corp.com", "charlie", t1, 50, 10, 60, null);

        commitJpaRepository.saveAllAndFlush(List.of(c1, c2, c3, c4));

        List<ContributorAggregationRow> rows = repositoryContributorJpaRepository.aggregateContributorsByRepositoryId(repo1.getId());

        assertThat(rows).hasSize(2);

        ContributorAggregationRow aliceRow = rows.stream()
                .filter(r -> r.getEmail().equals("alice@corp.com"))
                .findFirst()
                .orElseThrow();

        assertThat(aliceRow.getUsername()).isEqualTo("alice_dev");
        assertThat(aliceRow.getName()).isEqualTo("Alice Current");
        assertThat(aliceRow.getTotalCommits()).isEqualTo(2);
        assertThat(aliceRow.getTotalAdditions()).isEqualTo(30);
        assertThat(aliceRow.getTotalDeletions()).isEqualTo(7);
        assertThat(aliceRow.getTotalChanges()).isEqualTo(37);
        assertThat(aliceRow.getFirstCommittedAtInstant()).isEqualTo(t1);
        assertThat(aliceRow.getLastCommittedAtInstant()).isEqualTo(t3);

        ContributorAggregationRow bobRow = rows.stream()
                .filter(r -> r.getEmail().equals("bob@corp.com"))
                .findFirst()
                .orElseThrow();

        assertThat(bobRow.getUsername()).isEqualTo("bob_dev");
        assertThat(bobRow.getName()).isEqualTo("Bob Developer");
        assertThat(bobRow.getTotalCommits()).isEqualTo(1);
        assertThat(bobRow.getTotalChanges()).isEqualTo(6);
    }

    @Test
    @DisplayName("Should query repository contributors with database-backed pagination and sorting")
    void queryRepositoryContributorsPaginationAndSorting() {
        Contributor c1 = contributorJpaRepository.save(new Contributor("c1@test.com", "c1", "C1"));
        Contributor c2 = contributorJpaRepository.save(new Contributor("c2@test.com", "c2", "C2"));
        Contributor c3 = contributorJpaRepository.save(new Contributor("c3@test.com", "c3", "C3"));

        Instant now = Instant.now();
        repositoryContributorJpaRepository.save(new RepositoryContributor(repo1, c1, 10, 50, 10, 60, now, now));
        repositoryContributorJpaRepository.save(new RepositoryContributor(repo1, c2, 50, 200, 50, 250, now, now));
        repositoryContributorJpaRepository.save(new RepositoryContributor(repo1, c3, 25, 100, 20, 120, now, now));
        repositoryContributorJpaRepository.flush();

        Page<RepositoryContributor> page = repositoryContributorJpaRepository.findByRepositoryId(
                repo1.getId(),
                PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "totalCommits"))
        );

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getContributor().getEmail()).isEqualTo("c2@test.com");
        assertThat(page.getContent().get(0).getTotalCommits()).isEqualTo(50);
        assertThat(page.getContent().get(1).getContributor().getEmail()).isEqualTo("c3@test.com");
        assertThat(page.getContent().get(1).getTotalCommits()).isEqualTo(25);
    }

    @Test
    @DisplayName("Deleting repository cascades and deletes repository_contributors records")
    void cascadeDeleteOnRepositoryRemoval() {
        Contributor c = contributorJpaRepository.saveAndFlush(new Contributor("temp@test.com", "temp", "Temp"));
        repositoryContributorJpaRepository.saveAndFlush(
                new RepositoryContributor(repo1, c, 1, 1, 0, 1, Instant.now(), Instant.now())
        );

        assertThat(repositoryContributorJpaRepository.count()).isEqualTo(1);

        repositoryJpaRepository.delete(repo1);
        repositoryJpaRepository.flush();

        assertThat(repositoryContributorJpaRepository.count()).isEqualTo(0);
        // Contributor entity itself is preserved
        assertThat(contributorJpaRepository.count()).isEqualTo(1);
    }
}
