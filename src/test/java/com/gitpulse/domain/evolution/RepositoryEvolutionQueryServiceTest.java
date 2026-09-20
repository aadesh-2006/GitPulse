package com.gitpulse.domain.evolution;

import com.gitpulse.common.exception.ResourceNotFoundException;
import com.gitpulse.domain.evolution.dto.RepositoryEvolutionResponse;
import com.gitpulse.domain.evolution.dto.RepositoryMonthlyEvolutionRow;
import com.gitpulse.domain.repository.RepositoryJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryEvolutionQueryServiceTest {

    @Mock
    private RepositoryJpaRepository repositoryJpaRepository;

    @Mock
    private RepositoryEvolutionJpaRepository evolutionJpaRepository;

    private RepositoryEvolutionQueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new RepositoryEvolutionQueryService(repositoryJpaRepository, evolutionJpaRepository);
    }

    @Test
    @DisplayName("Should successfully return repository evolution response with buckets")
    void getRepositoryEvolution_Success() {
        Long repositoryId = 1L;
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-03-01T00:00:00Z");

        when(repositoryJpaRepository.existsById(repositoryId)).thenReturn(true);

        RepositoryMonthlyEvolutionRow row1 = mock(RepositoryMonthlyEvolutionRow.class);
        when(row1.getBucketMonth()).thenReturn(java.time.OffsetDateTime.parse("2026-01-01T00:00:00Z"));
        when(row1.getTotalCommits()).thenReturn(5L);
        when(row1.getTotalAdditions()).thenReturn(100L);
        when(row1.getTotalDeletions()).thenReturn(20L);
        when(row1.getTotalChurn()).thenReturn(120L);
        when(row1.getActiveContributors()).thenReturn(2L);
        when(row1.getFilesChanged()).thenReturn(4L);
        when(row1.getFeatureCommits()).thenReturn(1L);
        when(row1.getBugFixCommits()).thenReturn(2L);
        when(row1.getRefactorCommits()).thenReturn(3L);
        when(row1.getDocumentationCommits()).thenReturn(4L);
        when(row1.getTestCommits()).thenReturn(5L);
        when(row1.getBuildCommits()).thenReturn(6L);
        when(row1.getConfigurationCommits()).thenReturn(7L);
        when(row1.getDependencyCommits()).thenReturn(8L);
        when(row1.getOtherCommits()).thenReturn(9L);

        RepositoryMonthlyEvolutionRow row2 = mock(RepositoryMonthlyEvolutionRow.class);
        when(row2.getBucketMonth()).thenReturn(java.time.OffsetDateTime.parse("2026-02-01T00:00:00Z"));
        when(row2.getTotalCommits()).thenReturn(0L);

        when(evolutionJpaRepository.findMonthlyEvolution(repositoryId, from, to))
                .thenReturn(List.of(row1, row2));

        RepositoryEvolutionResponse response = queryService.getRepositoryEvolution(repositoryId, from, to);

        assertThat(response).isNotNull();
        assertThat(response.repositoryId()).isEqualTo(repositoryId);
        assertThat(response.from()).isEqualTo(from);
        assertThat(response.to()).isEqualTo(to);
        assertThat(response.buckets()).hasSize(2);

        var b1 = response.buckets().get(0);
        assertThat(b1.month()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(b1.totalCommits()).isEqualTo(5L);
        assertThat(b1.totalAdditions()).isEqualTo(100L);
        assertThat(b1.totalDeletions()).isEqualTo(20L);
        assertThat(b1.totalChurn()).isEqualTo(120L);
        assertThat(b1.activeContributors()).isEqualTo(2L);
        assertThat(b1.filesChanged()).isEqualTo(4L);
        assertThat(b1.featureCommits()).isEqualTo(1L);
        assertThat(b1.bugFixCommits()).isEqualTo(2L);
        assertThat(b1.refactorCommits()).isEqualTo(3L);
        assertThat(b1.documentationCommits()).isEqualTo(4L);
        assertThat(b1.testCommits()).isEqualTo(5L);
        assertThat(b1.buildCommits()).isEqualTo(6L);
        assertThat(b1.configurationCommits()).isEqualTo(7L);
        assertThat(b1.dependencyCommits()).isEqualTo(8L);
        assertThat(b1.otherCommits()).isEqualTo(9L);

        var b2 = response.buckets().get(1);
        assertThat(b2.month()).isEqualTo(Instant.parse("2026-02-01T00:00:00Z"));
        assertThat(b2.totalCommits()).isEqualTo(0L);

        verify(repositoryJpaRepository).existsById(repositoryId);
        verify(evolutionJpaRepository).findMonthlyEvolution(repositoryId, from, to);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when from is null")
    void getRepositoryEvolution_NullFrom_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> queryService.getRepositoryEvolution(1L, null, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The 'from' timestamp parameter is required");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when to is null")
    void getRepositoryEvolution_NullTo_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> queryService.getRepositoryEvolution(1L, Instant.now(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The 'to' timestamp parameter is required");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when from is equal to to")
    void getRepositoryEvolution_FromEqualsTo_ThrowsIllegalArgumentException() {
        Instant now = Instant.now();
        assertThatThrownBy(() -> queryService.getRepositoryEvolution(1L, now, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The 'from' timestamp must be strictly before the 'to' timestamp");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when from is after to")
    void getRepositoryEvolution_FromAfterTo_ThrowsIllegalArgumentException() {
        Instant from = Instant.parse("2026-06-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-01T00:00:00Z");
        assertThatThrownBy(() -> queryService.getRepositoryEvolution(1L, from, to))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The 'from' timestamp must be strictly before the 'to' timestamp");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when repository does not exist")
    void getRepositoryEvolution_RepoNotFound_ThrowsResourceNotFoundException() {
        Long repositoryId = 999L;
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-03-01T00:00:00Z");

        when(repositoryJpaRepository.existsById(repositoryId)).thenReturn(false);

        assertThatThrownBy(() -> queryService.getRepositoryEvolution(repositoryId, from, to))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Repository not found with id: 999");
    }
}
