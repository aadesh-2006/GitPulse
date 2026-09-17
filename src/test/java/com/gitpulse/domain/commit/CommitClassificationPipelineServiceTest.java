package com.gitpulse.domain.commit;

import com.gitpulse.common.exception.ResourceNotFoundException;
import com.gitpulse.domain.commit.dto.CommitClassificationResult;
import com.gitpulse.domain.repository.Repository;
import com.gitpulse.domain.repository.RepositoryJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommitClassificationPipelineServiceTest {

    @Mock
    private RepositoryJpaRepository repositoryJpaRepository;

    @Mock
    private CommitJpaRepository commitJpaRepository;

    private CommitClassificationService commitClassificationService;
    private CommitClassificationPipelineService pipelineService;

    private Repository sampleRepository;
    private Repository otherRepository;

    @BeforeEach
    void setUp() {
        commitClassificationService = new CommitClassificationService();
        pipelineService = new CommitClassificationPipelineService(
                repositoryJpaRepository,
                commitJpaRepository,
                commitClassificationService
        );

        sampleRepository = new Repository("facebook", "react", "A JavaScript library for building user interfaces", "main");
        ReflectionTestUtils.setField(sampleRepository, "id", 1L);

        otherRepository = new Repository("vuejs", "core", "Vue.js core", "main");
        ReflectionTestUtils.setField(otherRepository, "id", 2L);
    }

    @Test
    @DisplayName("Should find unclassified commits, classify deterministically, and batch persist")
    void classifyCommits_Success() {
        when(repositoryJpaRepository.findById(1L)).thenReturn(Optional.of(sampleRepository));

        Commit c1 = new Commit(sampleRepository, "sha1", "feat(hooks): add useId hook", "Dev", "dev@fb.com", "dev", Instant.now(), 10, 0, 10, "url1");
        Commit c2 = new Commit(sampleRepository, "sha2", "fix(reconciler): resolve fiber crash", "Dev", "dev@fb.com", "dev", Instant.now(), 5, 2, 7, "url2");
        Commit c3 = new Commit(sampleRepository, "sha3", "docs: update README", "Dev", "dev@fb.com", "dev", Instant.now(), 1, 1, 2, "url3");

        when(commitJpaRepository.findUnclassifiedByRepositoryId(eq(1L), any(Pageable.class)))
                .thenReturn(List.of(c1, c2, c3))
                .thenReturn(Collections.emptyList());

        CommitClassificationResult result = pipelineService.classifyCommits(1L, 100L);

        assertThat(result.totalCommitsProcessed()).isEqualTo(3);
        assertThat(result.classifiedCount()).isEqualTo(3);
        assertThat(result.durationMs()).isGreaterThanOrEqualTo(0);

        assertThat(c1.getClassification()).isEqualTo(CommitClassification.FEATURE);
        assertThat(c2.getClassification()).isEqualTo(CommitClassification.BUG_FIX);
        assertThat(c3.getClassification()).isEqualTo(CommitClassification.DOCUMENTATION);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Commit>> captor = ArgumentCaptor.forClass(List.class);
        verify(commitJpaRepository, times(1)).saveAll(captor.capture());

        List<Commit> saved = captor.getValue();
        assertThat(saved).containsExactly(c1, c2, c3);
    }

    @Test
    @DisplayName("Empty workload: should complete gracefully with 0 processed commits when no unclassified commits exist")
    void classifyCommits_EmptyWorkload() {
        when(repositoryJpaRepository.findById(1L)).thenReturn(Optional.of(sampleRepository));
        when(commitJpaRepository.findUnclassifiedByRepositoryId(eq(1L), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        CommitClassificationResult result = pipelineService.classifyCommits(1L, 100L);

        assertThat(result.totalCommitsProcessed()).isEqualTo(0);
        assertThat(result.classifiedCount()).isEqualTo(0);
        verify(commitJpaRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when repository does not exist")
    void classifyCommits_RepositoryNotFound_ThrowsException() {
        when(repositoryJpaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pipelineService.classifyCommits(999L, 100L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Repository not found with id: '999'");

        verify(commitJpaRepository, never()).findUnclassifiedByRepositoryId(any(), any());
    }

    @Test
    @DisplayName("Repository Isolation: queries commits strictly by repositoryId")
    void classifyCommits_RepositoryIsolation() {
        when(repositoryJpaRepository.findById(1L)).thenReturn(Optional.of(sampleRepository));
        when(commitJpaRepository.findUnclassifiedByRepositoryId(eq(1L), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        pipelineService.classifyCommits(1L, 100L);

        verify(commitJpaRepository).findUnclassifiedByRepositoryId(eq(1L), any(Pageable.class));
        verify(commitJpaRepository, never()).findUnclassifiedByRepositoryId(eq(2L), any(Pageable.class));
    }

    @Test
    @DisplayName("Batching: processes multiple batches when total unclassified commits exceed batch size")
    void classifyCommits_MultipleBatches() {
        when(repositoryJpaRepository.findById(1L)).thenReturn(Optional.of(sampleRepository));

        // Create 500 mock commits for batch 1, and 100 for batch 2
        List<Commit> batch1 = new java.util.ArrayList<>();
        for (int i = 0; i < 500; i++) {
            batch1.add(new Commit(sampleRepository, "sha_b1_" + i, "feat: feature " + i, "Dev", "dev@fb.com", "dev", Instant.now(), 1, 0, 1, "url"));
        }

        List<Commit> batch2 = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) {
            batch2.add(new Commit(sampleRepository, "sha_b2_" + i, "fix: fix " + i, "Dev", "dev@fb.com", "dev", Instant.now(), 1, 0, 1, "url"));
        }

        when(commitJpaRepository.findUnclassifiedByRepositoryId(eq(1L), any(Pageable.class)))
                .thenReturn(batch1)
                .thenReturn(batch2);

        CommitClassificationResult result = pipelineService.classifyCommits(1L, 100L);

        assertThat(result.totalCommitsProcessed()).isEqualTo(600);
        assertThat(result.classifiedCount()).isEqualTo(600);

        verify(commitJpaRepository, times(2)).saveAll(any());
    }

    @Test
    @DisplayName("Classification failure propagates up without being swallowed")
    void classifyCommits_DatabaseSaveFailure_Propagates() {
        when(repositoryJpaRepository.findById(1L)).thenReturn(Optional.of(sampleRepository));

        Commit c1 = new Commit(sampleRepository, "sha1", "feat: add feature", "Dev", "dev@fb.com", "dev", Instant.now(), 10, 0, 10, "url1");
        when(commitJpaRepository.findUnclassifiedByRepositoryId(eq(1L), any(Pageable.class)))
                .thenReturn(List.of(c1));

        when(commitJpaRepository.saveAll(any())).thenThrow(new RuntimeException("Database connection failure"));

        assertThatThrownBy(() -> pipelineService.classifyCommits(1L, 100L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database connection failure");
    }
}
