package com.gitpulse.domain.file;

import com.gitpulse.common.exception.ResourceNotFoundException;
import com.gitpulse.domain.contributor.Contributor;
import com.gitpulse.domain.contributor.ContributorJpaRepository;
import com.gitpulse.domain.file.dto.FilePrimaryContributorRow;
import com.gitpulse.domain.file.dto.RepositoryFileAggregationResult;
import com.gitpulse.domain.file.dto.RepositoryFileAggregationRow;
import com.gitpulse.domain.repository.Repository;
import com.gitpulse.domain.repository.RepositoryJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryFileAggregationServiceTest {

    @Mock
    private RepositoryJpaRepository repositoryJpaRepository;

    @Mock
    private RepositoryFileJpaRepository repositoryFileJpaRepository;

    @Mock
    private ContributorJpaRepository contributorJpaRepository;

    private RepositoryFileAggregationService aggregationService;

    private Repository repository;

    @BeforeEach
    void setUp() {
        aggregationService = new RepositoryFileAggregationService(
                repositoryJpaRepository,
                repositoryFileJpaRepository,
                contributorJpaRepository
        );
        repository = new Repository("octocat", "Hello-World", "Hello World repo", "main");
        ReflectionTestUtils.setField(repository, "id", 1L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when repository does not exist")
    void throwExceptionWhenRepositoryNotFound() {
        when(repositoryJpaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aggregationService.aggregateRepositoryFiles(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Repository not found with id: 999");
    }

    @Test
    @DisplayName("Should successfully aggregate files, map primary contributor, and save newly created records")
    void aggregateFilesAndCreateRecords() {
        when(repositoryJpaRepository.findById(1L)).thenReturn(Optional.of(repository));

        OffsetDateTime t1 = OffsetDateTime.of(2026, 9, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime t2 = OffsetDateTime.of(2026, 9, 5, 12, 0, 0, 0, ZoneOffset.UTC);

        RepositoryFileAggregationRow row = createMockAggRow("src/App.java", 5, 100, 20, 120, t1, t2, "MODIFIED");
        when(repositoryFileJpaRepository.aggregateFilesByRepositoryId(1L)).thenReturn(List.of(row));

        FilePrimaryContributorRow contribRow = createMockContribRow("src/App.java", 10L, 5, t2);
        when(repositoryFileJpaRepository.findPrimaryContributorsByRepositoryId(1L)).thenReturn(List.of(contribRow));

        Contributor contributor = new Contributor("dev@example.com", "dev", "Dev User");
        ReflectionTestUtils.setField(contributor, "id", 10L);
        when(contributorJpaRepository.findAllById(Set.of(10L))).thenReturn(List.of(contributor));

        when(repositoryFileJpaRepository.findByRepositoryId(1L)).thenReturn(Collections.emptyList());

        RepositoryFileAggregationResult result = aggregationService.aggregateRepositoryFiles(1L);

        assertThat(result.repositoryId()).isEqualTo(1L);
        assertThat(result.totalFilesProcessed()).isEqualTo(1);
        assertThat(result.createdCount()).isEqualTo(1);
        assertThat(result.updatedCount()).isEqualTo(0);
        assertThat(result.deletedCount()).isEqualTo(0);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RepositoryFile>> captor = ArgumentCaptor.forClass(List.class);
        verify(repositoryFileJpaRepository).saveAll(captor.capture());

        List<RepositoryFile> savedFiles = captor.getValue();
        assertThat(savedFiles).hasSize(1);
        RepositoryFile saved = savedFiles.get(0);
        assertThat(saved.getFilePath()).isEqualTo("src/App.java");
        assertThat(saved.getFileName()).isEqualTo("App.java");
        assertThat(saved.getExtension()).isEqualTo("java");
        assertThat(saved.getDirectoryPath()).isEqualTo("src");
        assertThat(saved.getTotalRevisions()).isEqualTo(5);
        assertThat(saved.getTotalAdditions()).isEqualTo(100);
        assertThat(saved.getTotalDeletions()).isEqualTo(20);
        assertThat(saved.getTotalChurn()).isEqualTo(120);
        assertThat(saved.isDeleted()).isFalse();
        assertThat(saved.getFirstModifiedAt()).isEqualTo(t1.toInstant());
        assertThat(saved.getLastModifiedAt()).isEqualTo(t2.toInstant());
        assertThat(saved.getPrimaryContributor()).isEqualTo(contributor);
    }

    @Test
    @DisplayName("Should update existing repository file in place and delete stale file records")
    void updateExistingFileAndDeleteStaleFiles() {
        when(repositoryJpaRepository.findById(1L)).thenReturn(Optional.of(repository));

        OffsetDateTime t1 = OffsetDateTime.of(2026, 9, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime t2 = OffsetDateTime.of(2026, 9, 2, 10, 0, 0, 0, ZoneOffset.UTC);

        RepositoryFileAggregationRow activeRow = createMockAggRow("active.txt", 3, 30, 5, 35, t1, t2, "REMOVED");
        when(repositoryFileJpaRepository.aggregateFilesByRepositoryId(1L)).thenReturn(List.of(activeRow));
        when(repositoryFileJpaRepository.findPrimaryContributorsByRepositoryId(1L)).thenReturn(Collections.emptyList());

        RepositoryFile existingActive = new RepositoryFile(
                repository, "active.txt", "active.txt", "txt", null,
                1, 10, 0, 10, false, t1.toInstant(), t1.toInstant(), null
        );
        RepositoryFile existingStale = new RepositoryFile(
                repository, "stale.txt", "stale.txt", "txt", null,
                1, 5, 0, 5, false, t1.toInstant(), t1.toInstant(), null
        );

        when(repositoryFileJpaRepository.findByRepositoryId(1L)).thenReturn(List.of(existingActive, existingStale));

        RepositoryFileAggregationResult result = aggregationService.aggregateRepositoryFiles(1L);

        assertThat(result.totalFilesProcessed()).isEqualTo(1);
        assertThat(result.createdCount()).isEqualTo(0);
        assertThat(result.updatedCount()).isEqualTo(1);
        assertThat(result.deletedCount()).isEqualTo(1);

        assertThat(existingActive.getTotalRevisions()).isEqualTo(3);
        assertThat(existingActive.getTotalAdditions()).isEqualTo(30);
        assertThat(existingActive.getTotalChurn()).isEqualTo(35);
        assertThat(existingActive.isDeleted()).isTrue();

        verify(repositoryFileJpaRepository).deleteAllInBatch(List.of(existingStale));
    }

    @Test
    @DisplayName("Should propagate runtime exceptions during materialization to allow rollback")
    void propagateExceptionsDuringMaterialization() {
        when(repositoryJpaRepository.findById(1L)).thenReturn(Optional.of(repository));

        OffsetDateTime t1 = OffsetDateTime.of(2026, 9, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        RepositoryFileAggregationRow row = createMockAggRow("src/App.java", 1, 10, 0, 10, t1, t1, "ADDED");
        when(repositoryFileJpaRepository.aggregateFilesByRepositoryId(1L)).thenReturn(List.of(row));
        when(repositoryFileJpaRepository.findPrimaryContributorsByRepositoryId(1L)).thenReturn(Collections.emptyList());
        when(repositoryFileJpaRepository.findByRepositoryId(1L)).thenReturn(Collections.emptyList());

        doThrow(new RuntimeException("Database write error")).when(repositoryFileJpaRepository).saveAll(anyList());

        assertThatThrownBy(() -> aggregationService.aggregateRepositoryFiles(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database write error");
    }

    @Test
    @DisplayName("Should handle empty repository gracefully without creating records")
    void handleEmptyRepository() {
        when(repositoryJpaRepository.findById(1L)).thenReturn(Optional.of(repository));
        when(repositoryFileJpaRepository.aggregateFilesByRepositoryId(1L)).thenReturn(Collections.emptyList());
        when(repositoryFileJpaRepository.findPrimaryContributorsByRepositoryId(1L)).thenReturn(Collections.emptyList());
        when(repositoryFileJpaRepository.findByRepositoryId(1L)).thenReturn(Collections.emptyList());

        RepositoryFileAggregationResult result = aggregationService.aggregateRepositoryFiles(1L);

        assertThat(result.totalFilesProcessed()).isEqualTo(0);
        assertThat(result.createdCount()).isEqualTo(0);
        assertThat(result.updatedCount()).isEqualTo(0);
        assertThat(result.deletedCount()).isEqualTo(0);

        verify(repositoryFileJpaRepository, never()).deleteAllInBatch(any());
    }

    private RepositoryFileAggregationRow createMockAggRow(String filePath, int revisions, int additions, int deletions,
                                                          int churn, OffsetDateTime firstAt, OffsetDateTime lastAt, String status) {
        return new RepositoryFileAggregationRow() {
            @Override
            public String getFilePath() { return filePath; }
            @Override
            public int getTotalRevisions() { return revisions; }
            @Override
            public int getTotalAdditions() { return additions; }
            @Override
            public int getTotalDeletions() { return deletions; }
            @Override
            public int getTotalChurn() { return churn; }
            @Override
            public OffsetDateTime getFirstModifiedAt() { return firstAt; }
            @Override
            public OffsetDateTime getLastModifiedAt() { return lastAt; }
            @Override
            public String getLastStatus() { return status; }
        };
    }

    private FilePrimaryContributorRow createMockContribRow(String filePath, Long contributorId, int count, OffsetDateTime latestAt) {
        return new FilePrimaryContributorRow() {
            @Override
            public String getFilePath() { return filePath; }
            @Override
            public Long getContributorId() { return contributorId; }
            @Override
            public int getContributionCount() { return count; }
            @Override
            public OffsetDateTime getLatestContributionAt() { return latestAt; }
        };
    }
}