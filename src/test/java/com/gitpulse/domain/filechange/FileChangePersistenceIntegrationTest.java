package com.gitpulse.domain.filechange;

import com.gitpulse.domain.commit.Commit;
import com.gitpulse.domain.commit.CommitJpaRepository;
import com.gitpulse.domain.repository.Repository;
import com.gitpulse.domain.repository.RepositoryJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class FileChangePersistenceIntegrationTest {

    @Autowired
    private FileChangeJpaRepository fileChangeJpaRepository;

    @Autowired
    private CommitJpaRepository commitJpaRepository;

    @Autowired
    private RepositoryJpaRepository repositoryJpaRepository;

    private Repository repo;
    private Commit commit1;
    private Commit commit2;

    @BeforeEach
    void setUp() {
        repo = repositoryJpaRepository.save(new Repository("owner", "repo", "Test Repo", "main"));
        commit1 = commitJpaRepository.save(new Commit(
                repo, "sha1111111111111111111111111111111111111", "Commit 1",
                "Alice", "alice@test.com", "alice", Instant.now(), 10, 2, 12, "url1"
        ));
        commit2 = commitJpaRepository.save(new Commit(
                repo, "sha2222222222222222222222222222222222222", "Commit 2",
                "Bob", "bob@test.com", "bob", Instant.now(), 5, 0, 5, "url2"
        ));
    }

    @Test
    @DisplayName("Should successfully persist and query FileChange entities")
    void persistAndQueryFileChange() {
        FileChange fc = new FileChange(
                commit1,
                "src/main/java/App.java",
                FileChangeStatus.MODIFIED,
                10,
                2,
                12,
                "https://github.com/owner/repo/blob/sha1/src/main/java/App.java",
                "https://github.com/owner/repo/raw/sha1/src/main/java/App.java"
        );

        FileChange saved = fileChangeJpaRepository.saveAndFlush(fc);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getFilePath()).isEqualTo("src/main/java/App.java");
        assertThat(saved.getStatus()).isEqualTo(FileChangeStatus.MODIFIED);
        assertThat(fileChangeJpaRepository.countByCommitId(commit1.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("Should enforce uniqueness on (commit_id, file_path)")
    void enforceUniqueConstraint() {
        FileChange fc1 = new FileChange(commit1, "src/Test.java", FileChangeStatus.ADDED, 5, 0, 5, null, null);
        fileChangeJpaRepository.saveAndFlush(fc1);

        FileChange fc2 = new FileChange(commit1, "src/Test.java", FileChangeStatus.MODIFIED, 2, 1, 3, null, null);

        assertThatThrownBy(() -> fileChangeJpaRepository.saveAndFlush(fc2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should allow same file_path across different commits")
    void allowSameFileDifferentCommits() {
        FileChange fc1 = new FileChange(commit1, "src/Common.java", FileChangeStatus.ADDED, 10, 0, 10, null, null);
        FileChange fc2 = new FileChange(commit2, "src/Common.java", FileChangeStatus.MODIFIED, 3, 1, 4, null, null);

        FileChange saved1 = fileChangeJpaRepository.saveAndFlush(fc1);
        FileChange saved2 = fileChangeJpaRepository.saveAndFlush(fc2);

        assertThat(saved1.getId()).isNotNull();
        assertThat(saved2.getId()).isNotNull();
        assertThat(saved1.getId()).isNotEqualTo(saved2.getId());
    }

    @Test
    @DisplayName("findCommitIdsWithFileChanges should return distinct commit IDs in batch")
    void findCommitIdsWithFileChanges() {
        FileChange fc1 = new FileChange(commit1, "src/A.java", FileChangeStatus.ADDED, 1, 0, 1, null, null);
        FileChange fc2 = new FileChange(commit1, "src/B.java", FileChangeStatus.ADDED, 2, 0, 2, null, null);
        fileChangeJpaRepository.saveAllAndFlush(List.of(fc1, fc2));

        Set<Long> commitIdsWithChanges = fileChangeJpaRepository.findCommitIdsWithFileChanges(
                List.of(commit1.getId(), commit2.getId())
        );

        assertThat(commitIdsWithChanges).containsExactly(commit1.getId());
        assertThat(commitIdsWithChanges).doesNotContain(commit2.getId());
    }

    @Test
    @DisplayName("findExistingFilePaths should return matching file paths for commit")
    void findExistingFilePaths() {
        FileChange fc1 = new FileChange(commit1, "src/A.java", FileChangeStatus.ADDED, 1, 0, 1, null, null);
        FileChange fc2 = new FileChange(commit1, "src/B.java", FileChangeStatus.ADDED, 2, 0, 2, null, null);
        fileChangeJpaRepository.saveAllAndFlush(List.of(fc1, fc2));

        List<String> existing = fileChangeJpaRepository.findExistingFilePaths(
                commit1.getId(),
                List.of("src/A.java", "src/C.java", "src/B.java")
        );

        assertThat(existing).containsExactlyInAnyOrder("src/A.java", "src/B.java");
    }

    @Test
    @DisplayName("Deleting repository cascades and deletes associated file changes")
    void cascadeDeleteOnRepositoryRemoval() {
        FileChange fc = new FileChange(commit1, "src/Main.java", FileChangeStatus.ADDED, 1, 0, 1, null, null);
        fileChangeJpaRepository.saveAndFlush(fc);

        assertThat(fileChangeJpaRepository.count()).isEqualTo(1);

        repositoryJpaRepository.delete(repo);
        repositoryJpaRepository.flush();

        assertThat(fileChangeJpaRepository.count()).isEqualTo(0);
        assertThat(commitJpaRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("findByCommitIdOrderByFilePathAsc should return file changes ordered by filePath ascending")
    void findByCommitIdOrderByFilePathAsc() {
        FileChange fcZ = new FileChange(commit1, "z_file.txt", FileChangeStatus.ADDED, 1, 0, 1, null, null);
        FileChange fcA = new FileChange(commit1, "a_file.txt", FileChangeStatus.MODIFIED, 2, 0, 2, null, null);
        FileChange fcM = new FileChange(commit1, "m_file.txt", FileChangeStatus.REMOVED, 0, 1, 1, null, null);
        fileChangeJpaRepository.saveAllAndFlush(List.of(fcZ, fcA, fcM));

        List<FileChange> results = fileChangeJpaRepository.findByCommitIdOrderByFilePathAsc(commit1.getId());

        assertThat(results).hasSize(3);
        assertThat(results.get(0).getFilePath()).isEqualTo("a_file.txt");
        assertThat(results.get(1).getFilePath()).isEqualTo("m_file.txt");
        assertThat(results.get(2).getFilePath()).isEqualTo("z_file.txt");
    }
}
