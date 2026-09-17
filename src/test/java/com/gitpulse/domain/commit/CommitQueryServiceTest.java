package com.gitpulse.domain.commit;

import com.gitpulse.common.exception.AppException;
import com.gitpulse.common.exception.ResourceNotFoundException;
import com.gitpulse.domain.commit.dto.CommitDetailResponse;
import com.gitpulse.domain.commit.dto.CommitResponse;
import com.gitpulse.domain.filechange.FileChange;
import com.gitpulse.domain.filechange.FileChangeJpaRepository;
import com.gitpulse.domain.filechange.FileChangeStatus;
import com.gitpulse.domain.repository.Repository;
import com.gitpulse.domain.repository.RepositoryJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommitQueryServiceTest {

    @Mock
    private RepositoryJpaRepository repositoryJpaRepository;

    @Mock
    private CommitJpaRepository commitJpaRepository;

    @Mock
    private FileChangeJpaRepository fileChangeJpaRepository;

    @InjectMocks
    private CommitQueryService commitQueryService;

    private Repository sampleRepo;
    private Commit sampleCommit;

    @BeforeEach
    void setUp() {
        sampleRepo = new Repository("owner", "repo", "Repo", "main");
        sampleCommit = new Commit(
                sampleRepo,
                "sha1234567890123456789012345678901234567",
                "feat: new feature",
                "Alice",
                "alice@corp.com",
                "alice",
                Instant.parse("2026-09-01T12:00:00Z"),
                10,
                2,
                12,
                "https://github.com/owner/repo/commit/sha123",
                CommitClassification.FEATURE
        );
    }

    @Test
    @DisplayName("getRepositoryCommits throws ResourceNotFoundException when repository does not exist")
    void getRepositoryCommits_RepoNotFound() {
        when(repositoryJpaRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> commitQueryService.getRepositoryCommits(999L, null, null, null, null, PageRequest.of(0, 20)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Repository not found with id: 999");
    }

    @Test
    @DisplayName("getRepositoryCommits rejects invalid sort field")
    void getRepositoryCommits_InvalidSortField() {
        when(repositoryJpaRepository.existsById(1L)).thenReturn(true);

        Pageable invalidPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "invalidField"));

        assertThatThrownBy(() -> commitQueryService.getRepositoryCommits(1L, null, null, null, null, invalidPageable))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Invalid sort field: 'invalidField'");
    }

    @Test
    @DisplayName("getRepositoryCommits returns mapped CommitResponse page")
    void getRepositoryCommits_Success() {
        when(repositoryJpaRepository.existsById(1L)).thenReturn(true);
        Page<Commit> page = new PageImpl<>(List.of(sampleCommit), PageRequest.of(0, 20), 1);
        when(commitJpaRepository.findByRepositoryIdWithFilters(eq(1L), eq(CommitClassification.FEATURE), eq("alice@corp.com"), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        Page<CommitResponse> result = commitQueryService.getRepositoryCommits(
                1L,
                CommitClassification.FEATURE,
                "  alice@corp.com  ",
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-09-02T00:00:00Z"),
                PageRequest.of(0, 20)
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        CommitResponse item = result.getContent().get(0);
        assertThat(item.githubCommitSha()).isEqualTo("sha1234567890123456789012345678901234567");
        assertThat(item.classification()).isEqualTo(CommitClassification.FEATURE);
        assertThat(item.authorEmail()).isEqualTo("alice@corp.com");
    }

    @Test
    @DisplayName("getCommitDetail throws ResourceNotFoundException when repository does not exist")
    void getCommitDetail_RepoNotFound() {
        when(commitJpaRepository.findByIdAndRepositoryId(10L, 999L)).thenReturn(Optional.empty());
        when(repositoryJpaRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> commitQueryService.getCommitDetail(999L, 10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Repository not found with id: 999");
    }

    @Test
    @DisplayName("getCommitDetail throws ResourceNotFoundException when commit does not belong to repository")
    void getCommitDetail_CommitNotFound() {
        when(commitJpaRepository.findByIdAndRepositoryId(10L, 1L)).thenReturn(Optional.empty());
        when(repositoryJpaRepository.existsById(1L)).thenReturn(true);

        assertThatThrownBy(() -> commitQueryService.getCommitDetail(1L, 10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Commit not found with id: 10 for repository id: 1");
    }

    @Test
    @DisplayName("getCommitDetail returns commit detail with ordered file changes")
    void getCommitDetail_Success() {
        when(commitJpaRepository.findByIdAndRepositoryId(10L, 1L)).thenReturn(Optional.of(sampleCommit));

        FileChange fc1 = new FileChange(sampleCommit, "src/A.java", FileChangeStatus.ADDED, 10, 0, 10, "blob1", "raw1");
        FileChange fc2 = new FileChange(sampleCommit, "src/B.java", FileChangeStatus.MODIFIED, 2, 1, 3, "blob2", "raw2");
        when(fileChangeJpaRepository.findByCommitIdOrderByFilePathAsc(10L)).thenReturn(List.of(fc1, fc2));

        CommitDetailResponse detail = commitQueryService.getCommitDetail(1L, 10L);

        assertThat(detail.githubCommitSha()).isEqualTo(sampleCommit.getGithubCommitSha());
        assertThat(detail.classification()).isEqualTo(CommitClassification.FEATURE);
        assertThat(detail.fileChanges()).hasSize(2);
        assertThat(detail.fileChanges().get(0).filePath()).isEqualTo("src/A.java");
        assertThat(detail.fileChanges().get(0).status()).isEqualTo(FileChangeStatus.ADDED);
        assertThat(detail.fileChanges().get(1).filePath()).isEqualTo("src/B.java");
        assertThat(detail.fileChanges().get(1).status()).isEqualTo(FileChangeStatus.MODIFIED);

        verify(fileChangeJpaRepository).findByCommitIdOrderByFilePathAsc(10L);
    }
}
