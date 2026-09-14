package com.gitpulse.domain.filechange;

import com.gitpulse.common.exception.ResourceNotFoundException;
import com.gitpulse.domain.commit.Commit;
import com.gitpulse.domain.commit.CommitJpaRepository;
import com.gitpulse.domain.filechange.dto.FileChangeIngestionResult;
import com.gitpulse.domain.repository.Repository;
import com.gitpulse.domain.repository.RepositoryJpaRepository;
import com.gitpulse.integration.github.client.GitHubCommitDetailsClient;
import com.gitpulse.integration.github.dto.GitHubCommitDetailResponse;
import com.gitpulse.integration.github.dto.GitHubCommitResponse;
import com.gitpulse.integration.github.dto.GitHubFileResponse;
import com.gitpulse.integration.github.exception.GitHubRateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileChangeIngestionServiceTest {

    @Mock
    private RepositoryJpaRepository repositoryJpaRepository;

    @Mock
    private CommitJpaRepository commitJpaRepository;

    @Mock
    private FileChangeJpaRepository fileChangeJpaRepository;

    @Mock
    private GitHubCommitDetailsClient gitHubCommitDetailsClient;

    @InjectMocks
    private FileChangeIngestionService fileChangeIngestionService;

    private Repository testRepository;
    private Commit commit1;
    private Commit commit2;

    @BeforeEach
    void setUp() {
        testRepository = new Repository("octocat", "Hello-World", "Sample repo", "main");
        ReflectionTestUtils.setField(testRepository, "id", 1L);

        commit1 = new Commit(testRepository, "sha111", "Commit 1", "Dev", "dev@test.com", "dev", Instant.now(), null, null, null, null);
        ReflectionTestUtils.setField(commit1, "id", 101L);

        commit2 = new Commit(testRepository, "sha222", "Commit 2", "Dev", "dev@test.com", "dev", Instant.now(), null, null, null, null);
        ReflectionTestUtils.setField(commit2, "id", 102L);
    }

    @Test
    @DisplayName("Should successfully ingest file changes for un-ingested commits and update commit stats")
    void ingestFileChangesSuccess() {
        when(repositoryJpaRepository.findById(1L)).thenReturn(Optional.of(testRepository));
        when(commitJpaRepository.findByRepositoryIdOrderByIdAsc(eq(1L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(commit1, commit2)));
        when(fileChangeJpaRepository.findCommitIdsWithFileChanges(List.of(101L, 102L)))
                .thenReturn(Collections.emptySet());

        GitHubFileResponse f1 = new GitHubFileResponse("src/A.java", "modified", 10, 2, 12, "blob1", "raw1", null);
        GitHubFileResponse f2 = new GitHubFileResponse("README.md", "added", 5, 0, 5, "blob2", "raw2", null);
        GitHubCommitResponse.CommitStats stats1 = new GitHubCommitResponse.CommitStats(15, 2, 17);
        GitHubCommitDetailResponse detail1 = new GitHubCommitDetailResponse("sha111", "url1", null, null, null, stats1, List.of(f1, f2));

        GitHubFileResponse f3 = new GitHubFileResponse("pom.xml", "modified", 1, 1, 2, "blob3", "raw3", null);
        GitHubCommitDetailResponse detail2 = new GitHubCommitDetailResponse("sha222", "url2", null, null, null, null, List.of(f3));

        when(gitHubCommitDetailsClient.getCommitDetails("octocat", "Hello-World", "sha111")).thenReturn(detail1);
        when(gitHubCommitDetailsClient.getCommitDetails("octocat", "Hello-World", "sha222")).thenReturn(detail2);

        FileChangeIngestionResult result = fileChangeIngestionService.ingestFileChanges(1L, 500L);

        assertThat(result.getCommitsProcessed()).isEqualTo(2);
        assertThat(result.getCommitsSkipped()).isEqualTo(0);
        assertThat(result.getFilesReceived()).isEqualTo(3);
        assertThat(result.getFilesInserted()).isEqualTo(3);
        assertThat(result.getDuplicatesEncountered()).isEqualTo(0);
        assertThat(result.getDurationMs()).isGreaterThanOrEqualTo(0);

        verify(commitJpaRepository, times(1)).save(commit1);
        assertThat(commit1.getAdditions()).isEqualTo(15);
        assertThat(commit1.getDeletions()).isEqualTo(2);
        assertThat(commit1.getTotalChanges()).isEqualTo(17);

        verify(fileChangeJpaRepository, times(2)).saveAll(anyList());
    }

    @Test
    @DisplayName("Should skip commits that already have file changes recorded")
    void skipAlreadyIngestedCommits() {
        when(repositoryJpaRepository.findById(1L)).thenReturn(Optional.of(testRepository));
        when(commitJpaRepository.findByRepositoryIdOrderByIdAsc(eq(1L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(commit1, commit2)));
        // commit1 already has file changes
        when(fileChangeJpaRepository.findCommitIdsWithFileChanges(List.of(101L, 102L)))
                .thenReturn(Set.of(101L));

        GitHubFileResponse f3 = new GitHubFileResponse("pom.xml", "modified", 1, 1, 2, "blob3", "raw3", null);
        GitHubCommitDetailResponse detail2 = new GitHubCommitDetailResponse("sha222", "url2", null, null, null, null, List.of(f3));

        when(gitHubCommitDetailsClient.getCommitDetails("octocat", "Hello-World", "sha222")).thenReturn(detail2);

        FileChangeIngestionResult result = fileChangeIngestionService.ingestFileChanges(1L, 500L);

        assertThat(result.getCommitsProcessed()).isEqualTo(1);
        assertThat(result.getCommitsSkipped()).isEqualTo(1);
        assertThat(result.getFilesReceived()).isEqualTo(1);
        assertThat(result.getFilesInserted()).isEqualTo(1);

        verify(gitHubCommitDetailsClient, never()).getCommitDetails("octocat", "Hello-World", "sha111");
        verify(fileChangeJpaRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("Should handle duplicate file paths within the same commit response")
    void handleDuplicateFilesWithinCommit() {
        when(repositoryJpaRepository.findById(1L)).thenReturn(Optional.of(testRepository));
        when(commitJpaRepository.findByRepositoryIdOrderByIdAsc(eq(1L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(commit1)));
        when(fileChangeJpaRepository.findCommitIdsWithFileChanges(List.of(101L)))
                .thenReturn(Collections.emptySet());

        GitHubFileResponse f1 = new GitHubFileResponse("src/A.java", "modified", 10, 2, 12, "blob1", "raw1", null);
        GitHubFileResponse f1Duplicate = new GitHubFileResponse("src/A.java", "modified", 10, 2, 12, "blob1", "raw1", null);
        GitHubCommitDetailResponse detail1 = new GitHubCommitDetailResponse("sha111", "url1", null, null, null, null, List.of(f1, f1Duplicate));

        when(gitHubCommitDetailsClient.getCommitDetails("octocat", "Hello-World", "sha111")).thenReturn(detail1);

        FileChangeIngestionResult result = fileChangeIngestionService.ingestFileChanges(1L, 500L);

        assertThat(result.getCommitsProcessed()).isEqualTo(1);
        assertThat(result.getFilesReceived()).isEqualTo(2);
        assertThat(result.getFilesInserted()).isEqualTo(1);
        assertThat(result.getDuplicatesEncountered()).isEqualTo(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FileChange>> captor = ArgumentCaptor.forClass(List.class);
        verify(fileChangeJpaRepository).saveAll(captor.capture());

        List<FileChange> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getFilePath()).isEqualTo("src/A.java");
    }

    @Test
    @DisplayName("Should handle empty files list without error")
    void handleEmptyFilesList() {
        when(repositoryJpaRepository.findById(1L)).thenReturn(Optional.of(testRepository));
        when(commitJpaRepository.findByRepositoryIdOrderByIdAsc(eq(1L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(commit1)));
        when(fileChangeJpaRepository.findCommitIdsWithFileChanges(List.of(101L)))
                .thenReturn(Collections.emptySet());

        GitHubCommitDetailResponse detail = new GitHubCommitDetailResponse("sha111", "url1", null, null, null, null, Collections.emptyList());
        when(gitHubCommitDetailsClient.getCommitDetails("octocat", "Hello-World", "sha111")).thenReturn(detail);

        FileChangeIngestionResult result = fileChangeIngestionService.ingestFileChanges(1L, 500L);

        assertThat(result.getCommitsProcessed()).isEqualTo(1);
        assertThat(result.getFilesReceived()).isEqualTo(0);
        assertThat(result.getFilesInserted()).isEqualTo(0);

        verify(fileChangeJpaRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should propagate GitHub rate limit exception and stop processing")
    void propagateGitHubRateLimit() {
        when(repositoryJpaRepository.findById(1L)).thenReturn(Optional.of(testRepository));
        when(commitJpaRepository.findByRepositoryIdOrderByIdAsc(eq(1L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(commit1)));
        when(fileChangeJpaRepository.findCommitIdsWithFileChanges(List.of(101L)))
                .thenReturn(Collections.emptySet());

        when(gitHubCommitDetailsClient.getCommitDetails("octocat", "Hello-World", "sha111"))
                .thenThrow(new GitHubRateLimitExceededException("Rate limit exceeded", 0, 1700000000L));

        assertThatThrownBy(() -> fileChangeIngestionService.ingestFileChanges(1L, 500L))
                .isInstanceOf(GitHubRateLimitExceededException.class);

        verify(fileChangeJpaRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when repository does not exist")
    void repositoryNotFound() {
        when(repositoryJpaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fileChangeIngestionService.ingestFileChanges(999L, 500L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(gitHubCommitDetailsClient, never()).getCommitDetails(anyString(), anyString(), anyString());
    }
}
