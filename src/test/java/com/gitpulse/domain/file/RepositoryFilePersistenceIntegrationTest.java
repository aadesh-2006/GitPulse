package com.gitpulse.domain.file;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class RepositoryFilePersistenceIntegrationTest {

    @Autowired
    private RepositoryFileJpaRepository repositoryFileJpaRepository;

    @Autowired
    private RepositoryJpaRepository repositoryJpaRepository;

    @Autowired
    private ContributorJpaRepository contributorJpaRepository;

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

        // Clear persistence context to test DB constraint execution
        entityManager.flush();
        entityManager.clear();

        // Delete contributor through DB / native or repository
        contributorJpaRepository.deleteById(contributor.getId());
        entityManager.flush();
        entityManager.clear();

        Optional<RepositoryFile> reloaded = repositoryFileJpaRepository.findById(fileId);
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getPrimaryContributor()).isNull();
    }

    @Test
    @DisplayName("Should query repository files with pagination")
    void queryRepositoryFilesWithPagination() {
        Instant now = Instant.now();
        repositoryFileJpaRepository.saveAndFlush(new RepositoryFile(repo1, "f1.java", "f1.java", "java", "", 1, 10, 0, 10, false, now, now, null));
        repositoryFileJpaRepository.saveAndFlush(new RepositoryFile(repo1, "f2.java", "f2.java", "java", "", 2, 20, 0, 20, false, now, now, null));
        repositoryFileJpaRepository.saveAndFlush(new RepositoryFile(repo1, "f3.java", "f3.java", "java", "", 3, 30, 0, 30, false, now, now, null));

        Page<RepositoryFile> page = repositoryFileJpaRepository.findByRepositoryId(repo1.getId(), PageRequest.of(0, 2));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(2);
    }
}