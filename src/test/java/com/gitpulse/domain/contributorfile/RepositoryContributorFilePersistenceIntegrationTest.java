package com.gitpulse.domain.contributorfile;

import com.gitpulse.domain.contributor.Contributor;
import com.gitpulse.domain.contributor.ContributorJpaRepository;
import com.gitpulse.domain.repository.Repository;
import com.gitpulse.domain.repository.RepositoryJpaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class RepositoryContributorFilePersistenceIntegrationTest {

    @Autowired
    private RepositoryContributorFileJpaRepository repositoryContributorFileJpaRepository;

    @Autowired
    private RepositoryJpaRepository repositoryJpaRepository;

    @Autowired
    private ContributorJpaRepository contributorJpaRepository;

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
        alice = contributorJpaRepository.save(new Contributor("alice@example.com", "alice", "Alice"));
        bob = contributorJpaRepository.save(new Contributor("bob@example.com", "bob", "Bob"));
    }

    @Test
    @DisplayName("Should persist RepositoryContributorFile with valid fields and timestamps")
    void persistRepositoryContributorFile() {
        Instant t1 = Instant.parse("2026-09-01T10:00:00Z");
        Instant t2 = Instant.parse("2026-09-05T12:00:00Z");

        RepositoryContributorFile rcf = new RepositoryContributorFile(
                repo1,
                alice,
                "src/main/java/com/gitpulse/App.java",
                12,
                150,
                30,
                180,
                t1,
                t2
        );

        RepositoryContributorFile saved = repositoryContributorFileJpaRepository.saveAndFlush(rcf);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getRepository().getId()).isEqualTo(repo1.getId());
        assertThat(saved.getContributor().getId()).isEqualTo(alice.getId());
        assertThat(saved.getFilePath()).isEqualTo("src/main/java/com/gitpulse/App.java");
        assertThat(saved.getTotalRevisions()).isEqualTo(12);
        assertThat(saved.getTotalAdditions()).isEqualTo(150);
        assertThat(saved.getTotalDeletions()).isEqualTo(30);
        assertThat(saved.getTotalChurn()).isEqualTo(180);
        assertThat(saved.getFirstContributedAt()).isEqualTo(t1);
        assertThat(saved.getLastContributedAt()).isEqualTo(t2);
    }

    @Test
    @DisplayName("Should enforce UNIQUE(repository_id, contributor_id, file_path) constraint")
    void enforceUniqueConstraint() {
        Instant now = Instant.now();
        RepositoryContributorFile rcf1 = new RepositoryContributorFile(
                repo1, alice, "src/Common.java", 5, 50, 10, 60, now, now
        );
        repositoryContributorFileJpaRepository.saveAndFlush(rcf1);

        RepositoryContributorFile rcfDuplicate = new RepositoryContributorFile(
                repo1, alice, "src/Common.java", 2, 20, 5, 25, now, now
        );

        assertThatThrownBy(() -> repositoryContributorFileJpaRepository.saveAndFlush(rcfDuplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should allow two different contributors to have records for the same file path")
    void allowDifferentContributorsSameFile() {
        Instant now = Instant.now();
        RepositoryContributorFile rcfAlice = new RepositoryContributorFile(
                repo1, alice, "src/Shared.java", 5, 50, 10, 60, now, now
        );
        RepositoryContributorFile rcfBob = new RepositoryContributorFile(
                repo1, bob, "src/Shared.java", 3, 30, 0, 30, now, now
        );

        RepositoryContributorFile saved1 = repositoryContributorFileJpaRepository.saveAndFlush(rcfAlice);
        RepositoryContributorFile saved2 = repositoryContributorFileJpaRepository.saveAndFlush(rcfBob);

        assertThat(saved1.getId()).isNotNull();
        assertThat(saved2.getId()).isNotNull();
        assertThat(saved1.getId()).isNotEqualTo(saved2.getId());
        assertThat(saved1.getFilePath()).isEqualTo(saved2.getFilePath());
    }

    @Test
    @DisplayName("Should allow one contributor to have records for multiple files")
    void allowOneContributorMultipleFiles() {
        Instant now = Instant.now();
        RepositoryContributorFile rcf1 = new RepositoryContributorFile(
                repo1, alice, "src/FileA.java", 1, 10, 0, 10, now, now
        );
        RepositoryContributorFile rcf2 = new RepositoryContributorFile(
                repo1, alice, "src/FileB.java", 2, 20, 5, 25, now, now
        );

        repositoryContributorFileJpaRepository.saveAllAndFlush(List.of(rcf1, rcf2));

        List<RepositoryContributorFile> files = repositoryContributorFileJpaRepository.findByRepositoryIdAndContributorId(repo1.getId(), alice.getId());
        assertThat(files).hasSize(2);
        assertThat(files).extracting(RepositoryContributorFile::getFilePath).containsExactlyInAnyOrder("src/FileA.java", "src/FileB.java");
    }

    @Test
    @DisplayName("Should allow same contributor and file across different repositories")
    void allowSameContributorAndFileInDifferentRepositories() {
        Instant now = Instant.now();
        RepositoryContributorFile rcfRepo1 = new RepositoryContributorFile(
                repo1, alice, "README.md", 2, 20, 0, 20, now, now
        );
        RepositoryContributorFile rcfRepo2 = new RepositoryContributorFile(
                repo2, alice, "README.md", 5, 50, 10, 60, now, now
        );

        RepositoryContributorFile saved1 = repositoryContributorFileJpaRepository.saveAndFlush(rcfRepo1);
        RepositoryContributorFile saved2 = repositoryContributorFileJpaRepository.saveAndFlush(rcfRepo2);

        assertThat(saved1.getId()).isNotNull();
        assertThat(saved2.getId()).isNotNull();
        assertThat(saved1.getId()).isNotEqualTo(saved2.getId());
    }

    @Test
    @DisplayName("Should cascade delete repository_contributor_files when repository is deleted")
    void cascadeDeleteOnRepositoryRemoval() {
        Instant now = Instant.now();
        repositoryContributorFileJpaRepository.saveAndFlush(new RepositoryContributorFile(
                repo1, alice, "src/A.java", 1, 10, 0, 10, now, now
        ));
        repositoryContributorFileJpaRepository.saveAndFlush(new RepositoryContributorFile(
                repo1, bob, "src/B.java", 2, 20, 0, 20, now, now
        ));

        assertThat(repositoryContributorFileJpaRepository.countByRepositoryId(repo1.getId())).isEqualTo(2);

        repositoryJpaRepository.delete(repo1);
        repositoryJpaRepository.flush();

        assertThat(repositoryContributorFileJpaRepository.countByRepositoryId(repo1.getId())).isEqualTo(0);
        assertThat(contributorJpaRepository.count()).isEqualTo(2); // Contributors preserved
    }

    @Test
    @DisplayName("Should cascade delete repository_contributor_files when contributor is deleted")
    void cascadeDeleteOnContributorRemoval() {
        Instant now = Instant.now();
        repositoryContributorFileJpaRepository.saveAndFlush(new RepositoryContributorFile(
                repo1, alice, "src/A.java", 1, 10, 0, 10, now, now
        ));

        assertThat(repositoryContributorFileJpaRepository.count()).isEqualTo(1);

        entityManager.flush();
        entityManager.clear();

        contributorJpaRepository.deleteById(alice.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(repositoryContributorFileJpaRepository.count()).isEqualTo(0);
        assertThat(repositoryJpaRepository.existsById(repo1.getId())).isTrue(); // Repository preserved
    }

    @Test
    @DisplayName("Lookup methods by repository, contributor and file path function correctly")
    void lookupMethods() {
        Instant now = Instant.now();
        RepositoryContributorFile rcf1 = new RepositoryContributorFile(
                repo1, alice, "src/Service.java", 4, 40, 10, 50, now, now
        );
        RepositoryContributorFile rcf2 = new RepositoryContributorFile(
                repo1, bob, "src/Service.java", 6, 60, 20, 80, now, now
        );
        repositoryContributorFileJpaRepository.saveAllAndFlush(List.of(rcf1, rcf2));

        // 1. By repository
        List<RepositoryContributorFile> repoFiles = repositoryContributorFileJpaRepository.findByRepositoryId(repo1.getId());
        assertThat(repoFiles).hasSize(2);

        // 2. By repository + contributor + file
        Optional<RepositoryContributorFile> single = repositoryContributorFileJpaRepository
                .findByRepositoryIdAndContributorIdAndFilePath(repo1.getId(), alice.getId(), "src/Service.java");
        assertThat(single).isPresent();
        assertThat(single.get().getTotalChurn()).isEqualTo(50);

        // 3. By repository + file
        List<RepositoryContributorFile> forFile = repositoryContributorFileJpaRepository
                .findByRepositoryIdAndFilePath(repo1.getId(), "src/Service.java");
        assertThat(forFile).hasSize(2);

        // 4. Delete by repository
        repositoryContributorFileJpaRepository.deleteByRepositoryId(repo1.getId());
        repositoryContributorFileJpaRepository.flush();
        assertThat(repositoryContributorFileJpaRepository.countByRepositoryId(repo1.getId())).isEqualTo(0);
    }
}
