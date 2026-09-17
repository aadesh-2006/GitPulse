package com.gitpulse.domain.contributorfile;

import com.gitpulse.common.exception.AppException;
import com.gitpulse.common.exception.ResourceNotFoundException;
import com.gitpulse.domain.contributor.Contributor;
import com.gitpulse.domain.contributor.ContributorJpaRepository;
import com.gitpulse.domain.contributorfile.dto.RepositoryContributorFileResponse;
import com.gitpulse.domain.repository.Repository;
import com.gitpulse.domain.repository.RepositoryJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
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
@Import(RepositoryContributorFileQueryService.class)
class RepositoryContributorFileQueryServiceIntegrationTest {

    @Autowired
    private RepositoryContributorFileQueryService queryService;

    @Autowired
    private RepositoryContributorFileJpaRepository repositoryContributorFileJpaRepository;

    @Autowired
    private RepositoryJpaRepository repositoryJpaRepository;

    @Autowired
    private ContributorJpaRepository contributorJpaRepository;

    private Repository repo1;
    private Repository repo2;
    private Contributor alice;
    private Contributor bob;
    private Contributor charlie;

    @BeforeEach
    void setUp() {
        repo1 = repositoryJpaRepository.save(new Repository("owner1", "repo1", "Repo 1", "main"));
        repo2 = repositoryJpaRepository.save(new Repository("owner2", "repo2", "Repo 2", "main"));
        alice = contributorJpaRepository.save(new Contributor("alice@example.com", "alice", "Alice"));
        bob = contributorJpaRepository.save(new Contributor("bob@example.com", "bob", "Bob"));
        charlie = contributorJpaRepository.save(new Contributor("charlie@example.com", "charlie", "Charlie"));

        Instant now = Instant.now();
        // Repo 1 data
        repositoryContributorFileJpaRepository.save(new RepositoryContributorFile(repo1, alice, "src/App.java", 10, 100, 20, 120, now, now));
        repositoryContributorFileJpaRepository.save(new RepositoryContributorFile(repo1, bob, "src/App.java", 5, 50, 10, 60, now, now));
        repositoryContributorFileJpaRepository.save(new RepositoryContributorFile(repo1, alice, "src/Util.java", 2, 20, 0, 20, now, now));

        // Repo 2 data (for isolation verification)
        repositoryContributorFileJpaRepository.save(new RepositoryContributorFile(repo2, alice, "src/App.java", 99, 990, 10, 1000, now, now));
        repositoryContributorFileJpaRepository.save(new RepositoryContributorFile(repo2, charlie, "src/Other.java", 1, 10, 0, 10, now, now));
        repositoryContributorFileJpaRepository.flush();
    }

    @Test
    @DisplayName("1. List contributor-files returns paginated records sorted by totalChurn DESC by default")
    void getRepositoryContributorFiles_DefaultSort() {
        Page<RepositoryContributorFileResponse> page = queryService.getRepositoryContributorFiles(
                repo1.getId(),
                PageRequest.of(0, 10)
        );

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(3);
        // Default sort is totalChurn DESC: 120, 60, 20
        assertThat(page.getContent().get(0).filePath()).isEqualTo("src/App.java");
        assertThat(page.getContent().get(0).contributor().id()).isEqualTo(alice.getId());
        assertThat(page.getContent().get(0).totalChurn()).isEqualTo(120);

        assertThat(page.getContent().get(1).filePath()).isEqualTo("src/App.java");
        assertThat(page.getContent().get(1).contributor().id()).isEqualTo(bob.getId());
        assertThat(page.getContent().get(1).totalChurn()).isEqualTo(60);

        assertThat(page.getContent().get(2).filePath()).isEqualTo("src/Util.java");
        assertThat(page.getContent().get(2).contributor().id()).isEqualTo(alice.getId());
        assertThat(page.getContent().get(2).totalChurn()).isEqualTo(20);
    }

    @Test
    @DisplayName("2. List contributor-files supports explicit sort fields")
    void getRepositoryContributorFiles_ExplicitSort() {
        Page<RepositoryContributorFileResponse> page = queryService.getRepositoryContributorFiles(
                repo1.getId(),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "filePath"))
        );

        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getContent().get(0).filePath()).isEqualTo("src/App.java");
        assertThat(page.getContent().get(1).filePath()).isEqualTo("src/App.java");
        assertThat(page.getContent().get(2).filePath()).isEqualTo("src/Util.java");
    }

    @Test
    @DisplayName("3. List contributor-files rejects invalid sort field with AppException")
    void getRepositoryContributorFiles_InvalidSort() {
        assertThatThrownBy(() -> queryService.getRepositoryContributorFiles(
                repo1.getId(),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "unsupportedColumn"))
        )).isInstanceOf(AppException.class)
                .hasMessageContaining("Invalid sort field: 'unsupportedColumn'");
    }

    @Test
    @DisplayName("4. List contributor-files throws ResourceNotFoundException for non-existent repository")
    void getRepositoryContributorFiles_NotFound() {
        assertThatThrownBy(() -> queryService.getRepositoryContributorFiles(
                999L,
                PageRequest.of(0, 10)
        )).isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Repository not found with id: 999");
    }

    @Test
    @DisplayName("5. Contributor-filtered query returns only records for that contributor in the repository")
    void getContributorFilesByContributor_Success() {
        Page<RepositoryContributorFileResponse> page = queryService.getContributorFilesByContributor(
                repo1.getId(),
                alice.getId(),
                PageRequest.of(0, 10)
        );

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(RepositoryContributorFileResponse::filePath)
                .containsExactlyInAnyOrder("src/App.java", "src/Util.java");
    }

    @Test
    @DisplayName("6. Contributor-filtered query throws 404 when contributor not found")
    void getContributorFilesByContributor_NotFound() {
        assertThatThrownBy(() -> queryService.getContributorFilesByContributor(
                repo1.getId(),
                999L,
                PageRequest.of(0, 10)
        )).isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Contributor not found with id: 999");
    }

    @Test
    @DisplayName("7. File-filtered query returns all contributor relationships for that file")
    void getContributorFilesByFilePath_Success() {
        Page<RepositoryContributorFileResponse> page = queryService.getContributorFilesByFilePath(
                repo1.getId(),
                "src/App.java",
                PageRequest.of(0, 10)
        );

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(r -> r.contributor().id())
                .containsExactlyInAnyOrder(alice.getId(), bob.getId());
    }

    @Test
    @DisplayName("8. File-filtered query throws AppException when filePath is blank")
    void getContributorFilesByFilePath_BlankThrows() {
        assertThatThrownBy(() -> queryService.getContributorFilesByFilePath(
                repo1.getId(),
                "  ",
                PageRequest.of(0, 10)
        )).isInstanceOf(AppException.class)
                .hasMessageContaining("filePath query parameter must not be blank");
    }

    @Test
    @DisplayName("9. Exact contributor-file lookup returns matching repo1/alice relationship (A)")
    void getContributorFile_Repo1Alice() {
        RepositoryContributorFileResponse response = queryService.getContributorFile(
                repo1.getId(),
                alice.getId(),
                "/src/App.java"
        );

        assertThat(response).isNotNull();
        assertThat(response.repositoryId()).isEqualTo(repo1.getId());
        assertThat(response.contributor().id()).isEqualTo(alice.getId());
        assertThat(response.filePath()).isEqualTo("src/App.java");
        assertThat(response.totalChurn()).isEqualTo(120);
    }

    @Test
    @DisplayName("10. Exact contributor-file lookup returns repo2/alice relationship and never repo1 record (B)")
    void getContributorFile_Repo2Alice() {
        RepositoryContributorFileResponse response = queryService.getContributorFile(
                repo2.getId(),
                alice.getId(),
                "src/App.java"
        );

        assertThat(response).isNotNull();
        assertThat(response.repositoryId()).isEqualTo(repo2.getId());
        assertThat(response.contributor().id()).isEqualTo(alice.getId());
        assertThat(response.filePath()).isEqualTo("src/App.java");
        assertThat(response.totalChurn()).isEqualTo(1000);
    }

    @Test
    @DisplayName("11. Exact contributor-file lookup returns repo1/bob relationship rather than alice (C)")
    void getContributorFile_Repo1Bob() {
        RepositoryContributorFileResponse response = queryService.getContributorFile(
                repo1.getId(),
                bob.getId(),
                "src/App.java"
        );

        assertThat(response).isNotNull();
        assertThat(response.repositoryId()).isEqualTo(repo1.getId());
        assertThat(response.contributor().id()).isEqualTo(bob.getId());
        assertThat(response.filePath()).isEqualTo("src/App.java");
        assertThat(response.totalChurn()).isEqualTo(60);
    }

    @Test
    @DisplayName("12. Exact contributor-file lookup throws 404 for non-existent file path in repo (D)")
    void getContributorFile_NonExistentFilePath() {
        assertThatThrownBy(() -> queryService.getContributorFile(
                repo1.getId(),
                alice.getId(),
                "src/DoesNotExist.java"
        )).isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Contributor-file relationship not found");
    }

    @Test
    @DisplayName("13. Exact contributor-file lookup throws 404 for non-existent contributor (E)")
    void getContributorFile_NonExistentContributor() {
        assertThatThrownBy(() -> queryService.getContributorFile(
                repo1.getId(),
                999L,
                "src/App.java"
        )).isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Contributor not found with id: 999");
    }

    @Test
    @DisplayName("14. Strict repository isolation: repo 2 records never leak into repo 1 queries")
    void repositoryIsolation() {
        Page<RepositoryContributorFileResponse> page1 = queryService.getRepositoryContributorFiles(
                repo1.getId(),
                PageRequest.of(0, 50)
        );
        assertThat(page1.getContent()).allMatch(r -> r.repositoryId().equals(repo1.getId()));
        assertThat(page1.getContent()).noneMatch(r -> r.contributor().id().equals(charlie.getId()));

        Page<RepositoryContributorFileResponse> page2 = queryService.getRepositoryContributorFiles(
                repo2.getId(),
                PageRequest.of(0, 50)
        );
        assertThat(page2.getContent()).allMatch(r -> r.repositoryId().equals(repo2.getId()));
    }

    @Test
    @DisplayName("15. File ownership calculation: multiple contributors on one file (Alice=10, Bob=5, Charlie=5)")
    void getRepositoryFileOwnership_MultipleContributors() {
        Repository repoConcentration = repositoryJpaRepository.save(new Repository("ownerC", "repoC", "Repo C", "main"));
        Instant now = Instant.now();

        // Alice = 10, Bob = 5, Charlie = 5 on src/Concentrated.java
        repositoryContributorFileJpaRepository.save(new RepositoryContributorFile(repoConcentration, alice, "src/Concentrated.java", 10, 100, 20, 120, now, now));
        repositoryContributorFileJpaRepository.save(new RepositoryContributorFile(repoConcentration, bob, "src/Concentrated.java", 5, 50, 10, 60, now, now));
        repositoryContributorFileJpaRepository.save(new RepositoryContributorFile(repoConcentration, charlie, "src/Concentrated.java", 5, 50, 10, 60, now, now));
        repositoryContributorFileJpaRepository.flush();

        Page<com.gitpulse.domain.contributorfile.dto.RepositoryFileOwnershipResponse> page =
                queryService.getRepositoryFileOwnership(repoConcentration.getId(), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        com.gitpulse.domain.contributorfile.dto.RepositoryFileOwnershipResponse record = page.getContent().get(0);

        assertThat(record.filePath()).isEqualTo("src/Concentrated.java");
        assertThat(record.contributorCount()).isEqualTo(3);
        assertThat(record.totalRevisionsAcrossContributors()).isEqualTo(20);
        assertThat(record.topContributor()).isNotNull();
        assertThat(record.topContributor().id()).isEqualTo(alice.getId());
        assertThat(record.topContributorRevisionShare()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("16. File ownership calculation: deterministic tie broken by lower contributorId")
    void getRepositoryFileOwnership_DeterministicTie() {
        Repository repoTie = repositoryJpaRepository.save(new Repository("ownerTie", "repoTie", "Repo Tie", "main"));
        Instant now = Instant.now();

        // Alice (lower ID) = 10, Bob (higher ID) = 10 on src/Tie.java
        repositoryContributorFileJpaRepository.save(new RepositoryContributorFile(repoTie, bob, "src/Tie.java", 10, 100, 20, 120, now, now));
        repositoryContributorFileJpaRepository.save(new RepositoryContributorFile(repoTie, alice, "src/Tie.java", 10, 100, 20, 120, now, now));
        repositoryContributorFileJpaRepository.flush();

        Page<com.gitpulse.domain.contributorfile.dto.RepositoryFileOwnershipResponse> page =
                queryService.getRepositoryFileOwnership(repoTie.getId(), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        com.gitpulse.domain.contributorfile.dto.RepositoryFileOwnershipResponse record = page.getContent().get(0);

        assertThat(record.topContributor().id()).isEqualTo(alice.getId()); // Alice has lower ID than Bob
        assertThat(record.topContributorRevisionShare()).isEqualTo(0.5);
        assertThat(record.contributorCount()).isEqualTo(2);
        assertThat(record.totalRevisionsAcrossContributors()).isEqualTo(20);
    }

    @Test
    @DisplayName("17. File ownership calculation: single contributor produces 1.0 share")
    void getRepositoryFileOwnership_SingleContributor() {
        Repository repoSingle = repositoryJpaRepository.save(new Repository("ownerS", "repoS", "Repo S", "main"));
        Instant now = Instant.now();

        repositoryContributorFileJpaRepository.save(new RepositoryContributorFile(repoSingle, alice, "src/Solo.java", 20, 200, 10, 210, now, now));
        repositoryContributorFileJpaRepository.flush();

        Page<com.gitpulse.domain.contributorfile.dto.RepositoryFileOwnershipResponse> page =
                queryService.getRepositoryFileOwnership(repoSingle.getId(), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        com.gitpulse.domain.contributorfile.dto.RepositoryFileOwnershipResponse record = page.getContent().get(0);

        assertThat(record.topContributor().id()).isEqualTo(alice.getId());
        assertThat(record.topContributorRevisionShare()).isEqualTo(1.0);
        assertThat(record.contributorCount()).isEqualTo(1);
        assertThat(record.totalRevisionsAcrossContributors()).isEqualTo(20);
    }

    @Test
    @DisplayName("18. File ownership calculation: multiple files aggregated independently with no duplicate rows")
    void getRepositoryFileOwnership_MultipleFiles() {
        Page<com.gitpulse.domain.contributorfile.dto.RepositoryFileOwnershipResponse> page =
                queryService.getRepositoryFileOwnership(repo1.getId(), PageRequest.of(0, 10));

        // repo1 has 2 distinct files: src/App.java (Alice=10, Bob=5) and src/Util.java (Alice=2)
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(com.gitpulse.domain.contributorfile.dto.RepositoryFileOwnershipResponse::filePath)
                .containsExactlyInAnyOrder("src/App.java", "src/Util.java");
    }

    @Test
    @DisplayName("19. File ownership repository isolation: repo 2 files never leak into repo 1")
    void getRepositoryFileOwnership_RepositoryIsolation() {
        Page<com.gitpulse.domain.contributorfile.dto.RepositoryFileOwnershipResponse> page1 =
                queryService.getRepositoryFileOwnership(repo1.getId(), PageRequest.of(0, 50));
        assertThat(page1.getContent()).allMatch(r -> r.repositoryId().equals(repo1.getId()));
        assertThat(page1.getContent()).noneMatch(r -> r.filePath().equals("src/Other.java"));

        Page<com.gitpulse.domain.contributorfile.dto.RepositoryFileOwnershipResponse> page2 =
                queryService.getRepositoryFileOwnership(repo2.getId(), PageRequest.of(0, 50));
        assertThat(page2.getContent()).allMatch(r -> r.repositoryId().equals(repo2.getId()));
    }

    @Test
    @DisplayName("20. File ownership invalid sort rejects with AppException")
    void getRepositoryFileOwnership_InvalidSort() {
        assertThatThrownBy(() -> queryService.getRepositoryFileOwnership(
                repo1.getId(),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "unsupportedColumn"))
        )).isInstanceOf(AppException.class)
                .hasMessageContaining("Invalid sort field: 'unsupportedColumn'");
    }

    @Test
    @DisplayName("21. File ownership explicit sorting: ASC and DESC on topContributorRevisionShare with deterministic secondary sort")
    void getRepositoryFileOwnership_ExplicitSorting_TopContributorRevisionShare() {
        Repository repoSort = repositoryJpaRepository.save(new Repository("ownerSort", "repoSort", "Repo Sort", "main"));
        Instant now = Instant.now();

        // fileA: Alice=5, Bob=5 -> total=10 -> share=0.50
        repositoryContributorFileJpaRepository.save(new RepositoryContributorFile(repoSort, alice, "src/fileA.java", 5, 50, 0, 50, now, now));
        repositoryContributorFileJpaRepository.save(new RepositoryContributorFile(repoSort, bob, "src/fileA.java", 5, 50, 0, 50, now, now));

        // fileB: Alice=10 -> total=10 -> share=1.00
        repositoryContributorFileJpaRepository.save(new RepositoryContributorFile(repoSort, alice, "src/fileB.java", 10, 100, 0, 100, now, now));

        // fileC: Alice=15, Bob=5 -> total=20 -> share=0.75
        repositoryContributorFileJpaRepository.save(new RepositoryContributorFile(repoSort, alice, "src/fileC.java", 15, 150, 0, 150, now, now));
        repositoryContributorFileJpaRepository.save(new RepositoryContributorFile(repoSort, bob, "src/fileC.java", 5, 50, 0, 50, now, now));

        // fileD: Alice=10, Bob=10 -> total=20 -> share=0.50 (Tied with fileA on share, but different filePath)
        repositoryContributorFileJpaRepository.save(new RepositoryContributorFile(repoSort, alice, "src/fileD.java", 10, 100, 0, 100, now, now));
        repositoryContributorFileJpaRepository.save(new RepositoryContributorFile(repoSort, bob, "src/fileD.java", 10, 100, 0, 100, now, now));

        repositoryContributorFileJpaRepository.flush();

        // ASC sort: fileA (0.50), fileD (0.50), fileC (0.75), fileB (1.00) (Deterministic secondary sort on filePath ASC)
        Page<com.gitpulse.domain.contributorfile.dto.RepositoryFileOwnershipResponse> pageAsc =
                queryService.getRepositoryFileOwnership(
                        repoSort.getId(),
                        PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "topContributorRevisionShare"))
                );

        assertThat(pageAsc.getContent()).extracting(com.gitpulse.domain.contributorfile.dto.RepositoryFileOwnershipResponse::filePath)
                .containsExactly("src/fileA.java", "src/fileD.java", "src/fileC.java", "src/fileB.java");
        assertThat(pageAsc.getContent()).extracting(com.gitpulse.domain.contributorfile.dto.RepositoryFileOwnershipResponse::topContributorRevisionShare)
                .containsExactly(0.5, 0.5, 0.75, 1.0);

        // DESC sort: fileB (1.00), fileC (0.75), fileA (0.50), fileD (0.50)
        Page<com.gitpulse.domain.contributorfile.dto.RepositoryFileOwnershipResponse> pageDesc =
                queryService.getRepositoryFileOwnership(
                        repoSort.getId(),
                        PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "topContributorRevisionShare"))
                );

        assertThat(pageDesc.getContent()).extracting(com.gitpulse.domain.contributorfile.dto.RepositoryFileOwnershipResponse::filePath)
                .containsExactly("src/fileB.java", "src/fileC.java", "src/fileA.java", "src/fileD.java");
        assertThat(pageDesc.getContent()).extracting(com.gitpulse.domain.contributorfile.dto.RepositoryFileOwnershipResponse::topContributorRevisionShare)
                .containsExactly(1.0, 0.75, 0.5, 0.5);
    }

    @Test
    @DisplayName("22. File ownership explicit sorting on other allowed fields: contributorCount and totalRevisionsAcrossContributors")
    void getRepositoryFileOwnership_ExplicitSorting_OtherFields() {
        Repository repoSort2 = repositoryJpaRepository.save(new Repository("ownerSort2", "repoSort2", "Repo Sort 2", "main"));
        Instant now = Instant.now();

        // file1: 1 contributor (Alice=1 revision)
        repositoryContributorFileJpaRepository.save(new RepositoryContributorFile(repoSort2, alice, "src/file1.java", 1, 10, 0, 10, now, now));

        // file2: 3 contributors (Alice=2, Bob=3, Charlie=5 -> total=10 revisions)
        repositoryContributorFileJpaRepository.save(new RepositoryContributorFile(repoSort2, alice, "src/file2.java", 2, 20, 0, 20, now, now));
        repositoryContributorFileJpaRepository.save(new RepositoryContributorFile(repoSort2, bob, "src/file2.java", 3, 30, 0, 30, now, now));
        repositoryContributorFileJpaRepository.save(new RepositoryContributorFile(repoSort2, charlie, "src/file2.java", 5, 50, 0, 50, now, now));

        // file3: 2 contributors (Alice=50, Bob=50 -> total=100 revisions)
        repositoryContributorFileJpaRepository.save(new RepositoryContributorFile(repoSort2, alice, "src/file3.java", 50, 500, 0, 500, now, now));
        repositoryContributorFileJpaRepository.save(new RepositoryContributorFile(repoSort2, bob, "src/file3.java", 50, 500, 0, 500, now, now));

        repositoryContributorFileJpaRepository.flush();

        // Sort by contributorCount ASC: file1 (1), file3 (2), file2 (3)
        Page<com.gitpulse.domain.contributorfile.dto.RepositoryFileOwnershipResponse> pageByCount =
                queryService.getRepositoryFileOwnership(
                        repoSort2.getId(),
                        PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "contributorCount"))
                );
        assertThat(pageByCount.getContent()).extracting(com.gitpulse.domain.contributorfile.dto.RepositoryFileOwnershipResponse::filePath)
                .containsExactly("src/file1.java", "src/file3.java", "src/file2.java");

        // Sort by totalRevisionsAcrossContributors DESC: file3 (100), file2 (10), file1 (1)
        Page<com.gitpulse.domain.contributorfile.dto.RepositoryFileOwnershipResponse> pageByRevisions =
                queryService.getRepositoryFileOwnership(
                        repoSort2.getId(),
                        PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "totalRevisionsAcrossContributors"))
                );
        assertThat(pageByRevisions.getContent()).extracting(com.gitpulse.domain.contributorfile.dto.RepositoryFileOwnershipResponse::filePath)
                .containsExactly("src/file3.java", "src/file2.java", "src/file1.java");

        // Sort by filePath ASC: file1, file2, file3
        Page<com.gitpulse.domain.contributorfile.dto.RepositoryFileOwnershipResponse> pageByPath =
                queryService.getRepositoryFileOwnership(
                        repoSort2.getId(),
                        PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "filePath"))
                );
        assertThat(pageByPath.getContent()).extracting(com.gitpulse.domain.contributorfile.dto.RepositoryFileOwnershipResponse::filePath)
                .containsExactly("src/file1.java", "src/file2.java", "src/file3.java");
    }
}
