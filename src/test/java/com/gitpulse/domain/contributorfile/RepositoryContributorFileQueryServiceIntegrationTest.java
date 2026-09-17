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
}
