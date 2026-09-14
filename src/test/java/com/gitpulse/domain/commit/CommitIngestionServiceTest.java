package com.gitpulse.domain.commit;

import com.gitpulse.common.exception.ResourceNotFoundException;
import com.gitpulse.domain.commit.dto.CommitIngestionResult;
import com.gitpulse.domain.repository.Repository;
import com.gitpulse.domain.repository.RepositoryJpaRepository;
import com.gitpulse.integration.github.client.GitHubCommitClient;
import com.gitpulse.integration.github.config.GitHubProperties;
import com.gitpulse.integration.github.dto.GitHubCommitPageResponse;
import com.gitpulse.integration.github.dto.GitHubCommitResponse;
import com.gitpulse.integration.github.exception.GitHubRateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommitIngestionServiceTest {

    @Mock
    private RepositoryJpaRepository repositoryJpaRepository;

    @Mock
    private CommitJpaRepository commitJpaRepository;

    @Mock
    private GitHubCommitClient gitHubCommitClient;

    @Mock
    private GitHubProperties gitHubProperties;

    @InjectMocks
    private CommitIngestionService commitIngestionService;

    private Repository testRepository;

    @BeforeEach
    void setUp() {
        testRepository = new Repository("octocat", "Hello-World", "Sample repo", "main");
        ReflectionTestUtils.setField(testRepository, "id", 1L);
    }

    @Test
    @DisplayName("Should ingest multiple pages of commits and persist them in batches")
    void ingestMultiplePages() {
        when(repositoryJpaRepository.findById(1L)).thenReturn(Optional.of(testRepository));
        when(gitHubProperties.getCommitPageSize()).thenReturn(30);

        GitHubCommitResponse commit1 = createCommitResponse("sha1", "Message 1", "Dev 1", "dev1@test.com", "dev1", Instant.now(), 10, 2, 12);
        GitHubCommitResponse commit2 = createCommitResponse("sha2", "Message 2", "Dev 2", "dev2@test.com", "dev2", Instant.now(), 5, 0, 5);
        GitHubCommitResponse commit3 = createCommitResponse("sha3", "Message 3", "Dev 3", "dev3@test.com", null, Instant.now(), null, null, null);

        GitHubCommitPageResponse page1 = new GitHubCommitPageResponse(List.of(commit1, commit2), true);
        GitHubCommitPageResponse page2 = new GitHubCommitPageResponse(List.of(commit3), false);

        when(gitHubCommitClient.getCommitsPage("octocat", "Hello-World", 1, 30)).thenReturn(page1);
        when(gitHubCommitClient.getCommitsPage("octocat", "Hello-World", 2, 30)).thenReturn(page2);
        when(commitJpaRepository.findExistingGithubCommitShas(eq(1L), anyCollection())).thenReturn(Collections.emptyList());

        CommitIngestionResult result = commitIngestionService.ingestCommits(1L, 100L);

        assertThat(result.getPagesProcessed()).isEqualTo(2);
        assertThat(result.getCommitsReceived()).isEqualTo(3);
        assertThat(result.getCommitsInserted()).isEqualTo(3);
        assertThat(result.getDuplicatesEncountered()).isEqualTo(0);
        assertThat(result.getDurationMs()).isGreaterThanOrEqualTo(0);

        verify(commitJpaRepository, times(2)).saveAll(any());
    }

    @Test
    @DisplayName("Should skip existing commit SHAs and only insert new commits")
    void skipDuplicateCommits() {
        when(repositoryJpaRepository.findById(1L)).thenReturn(Optional.of(testRepository));
        when(gitHubProperties.getCommitPageSize()).thenReturn(30);

        GitHubCommitResponse commit1 = createCommitResponse("sha1", "Message 1", "Dev 1", "dev1@test.com", "dev1", Instant.now(), null, null, null);
        GitHubCommitResponse commit2 = createCommitResponse("sha2", "Message 2", "Dev 2", "dev2@test.com", "dev2", Instant.now(), null, null, null);

        GitHubCommitPageResponse page = new GitHubCommitPageResponse(List.of(commit1, commit2), false);

        when(gitHubCommitClient.getCommitsPage("octocat", "Hello-World", 1, 30)).thenReturn(page);
        // sha1 already exists in DB
        when(commitJpaRepository.findExistingGithubCommitShas(1L, List.of("sha1", "sha2"))).thenReturn(List.of("sha1"));

        CommitIngestionResult result = commitIngestionService.ingestCommits(1L, 100L);

        assertThat(result.getPagesProcessed()).isEqualTo(1);
        assertThat(result.getCommitsReceived()).isEqualTo(2);
        assertThat(result.getCommitsInserted()).isEqualTo(1);
        assertThat(result.getDuplicatesEncountered()).isEqualTo(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Commit>> captor = ArgumentCaptor.forClass(List.class);
        verify(commitJpaRepository).saveAll(captor.capture());

        List<Commit> savedCommits = captor.getValue();
        assertThat(savedCommits).hasSize(1);
        assertThat(savedCommits.get(0).getGithubCommitSha()).isEqualTo("sha2");
    }

    @Test
    @DisplayName("Should skip intra-page duplicate commit SHAs when GitHub returns identical SHA multiple times in one page")
    void skipIntraPageDuplicateCommits() {
        when(repositoryJpaRepository.findById(1L)).thenReturn(Optional.of(testRepository));
        when(gitHubProperties.getCommitPageSize()).thenReturn(30);

        GitHubCommitResponse commit1 = createCommitResponse("sha1", "Message 1", "Dev 1", "dev1@test.com", "dev1", Instant.now(), null, null, null);
        GitHubCommitResponse duplicateCommit1 = createCommitResponse("sha1", "Message 1 duplicate", "Dev 1", "dev1@test.com", "dev1", Instant.now(), null, null, null);
        GitHubCommitResponse commit2 = createCommitResponse("sha2", "Message 2", "Dev 2", "dev2@test.com", "dev2", Instant.now(), null, null, null);

        GitHubCommitPageResponse page = new GitHubCommitPageResponse(List.of(commit1, duplicateCommit1, commit2), false);

        when(gitHubCommitClient.getCommitsPage("octocat", "Hello-World", 1, 30)).thenReturn(page);
        when(commitJpaRepository.findExistingGithubCommitShas(1L, List.of("sha1", "sha1", "sha2"))).thenReturn(Collections.emptyList());

        CommitIngestionResult result = commitIngestionService.ingestCommits(1L, 100L);

        assertThat(result.getPagesProcessed()).isEqualTo(1);
        assertThat(result.getCommitsReceived()).isEqualTo(3);
        assertThat(result.getCommitsInserted()).isEqualTo(2);
        assertThat(result.getDuplicatesEncountered()).isEqualTo(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Commit>> captor = ArgumentCaptor.forClass(List.class);
        verify(commitJpaRepository).saveAll(captor.capture());

        List<Commit> savedCommits = captor.getValue();
        assertThat(savedCommits).hasSize(2);
        assertThat(savedCommits.stream().map(Commit::getGithubCommitSha)).containsExactly("sha1", "sha2");
    }

    @Test
    @DisplayName("Should handle empty repository gracefully without inserting commits")
    void emptyRepository() {
        when(repositoryJpaRepository.findById(1L)).thenReturn(Optional.of(testRepository));
        when(gitHubProperties.getCommitPageSize()).thenReturn(30);

        GitHubCommitPageResponse emptyPage = new GitHubCommitPageResponse(Collections.emptyList(), false);
        when(gitHubCommitClient.getCommitsPage("octocat", "Hello-World", 1, 30)).thenReturn(emptyPage);

        CommitIngestionResult result = commitIngestionService.ingestCommits(1L, 100L);

        assertThat(result.getPagesProcessed()).isEqualTo(0);
        assertThat(result.getCommitsReceived()).isEqualTo(0);
        assertThat(result.getCommitsInserted()).isEqualTo(0);
        assertThat(result.getDuplicatesEncountered()).isEqualTo(0);

        verify(commitJpaRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Should propagate GitHub API exceptions when fetching commits fails")
    void propagateGitHubException() {
        when(repositoryJpaRepository.findById(1L)).thenReturn(Optional.of(testRepository));
        when(gitHubProperties.getCommitPageSize()).thenReturn(30);

        when(gitHubCommitClient.getCommitsPage("octocat", "Hello-World", 1, 30))
                .thenThrow(new GitHubRateLimitExceededException("Rate limit exceeded", 0, 1700000000L));

        assertThatThrownBy(() -> commitIngestionService.ingestCommits(1L, 100L))
                .isInstanceOf(GitHubRateLimitExceededException.class)
                .hasMessageContaining("Rate limit exceeded");

        verify(commitJpaRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when repository does not exist")
    void repositoryNotFound() {
        when(repositoryJpaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commitIngestionService.ingestCommits(999L, 100L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(gitHubCommitClient, never()).getCommitsPage(anyString(), anyString(), anyInt(), anyInt());
    }

    private GitHubCommitResponse createCommitResponse(String sha,
                                                      String message,
                                                      String authorName,
                                                      String authorEmail,
                                                      String authorUsername,
                                                      Instant date,
                                                      Integer additions,
                                                      Integer deletions,
                                                      Integer total) {
        GitHubCommitResponse.GitUser gitUser = new GitHubCommitResponse.GitUser(authorName, authorEmail, date);
        GitHubCommitResponse.CommitDetails details = new GitHubCommitResponse.CommitDetails(message, gitUser, gitUser);
        GitHubCommitResponse.GitHubUser ghUser = authorUsername != null ? new GitHubCommitResponse.GitHubUser(authorUsername, 1L) : null;
        GitHubCommitResponse.CommitStats stats = (additions != null || deletions != null || total != null)
                ? new GitHubCommitResponse.CommitStats(additions, deletions, total) : null;

        return new GitHubCommitResponse(sha, "https://github.com/octocat/Hello-World/commit/" + sha, details, ghUser, ghUser, stats);
    }
}
