package com.gitpulse.domain.evolution;

import com.gitpulse.common.exception.ResourceNotFoundException;
import com.gitpulse.domain.evolution.dto.RepositoryEvolutionCompositionResponse;
import com.gitpulse.domain.evolution.dto.RepositoryEvolutionPeriodSummaryRow;
import com.gitpulse.domain.repository.RepositoryJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryEvolutionCompositionQueryServiceTest {

    @Mock
    private RepositoryJpaRepository repositoryJpaRepository;

    @Mock
    private RepositoryEvolutionJpaRepository evolutionJpaRepository;

    private RepositoryEvolutionCompositionQueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new RepositoryEvolutionCompositionQueryService(repositoryJpaRepository, evolutionJpaRepository);
    }

    @Test
    @DisplayName("Should successfully compute composition shares and intensity metrics for active period")
    void getEvolutionComposition_Success() {
        Long repositoryId = 1L;
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-02-01T00:00:00Z");

        when(repositoryJpaRepository.existsById(repositoryId)).thenReturn(true);

        RepositoryEvolutionPeriodSummaryRow row = mock(RepositoryEvolutionPeriodSummaryRow.class);
        // Total commits = 10, of which 8 classified and 2 unclassified
        when(row.getTotalCommits()).thenReturn(10L);
        when(row.getTotalAdditions()).thenReturn(300L);
        when(row.getTotalDeletions()).thenReturn(100L);
        when(row.getTotalChurn()).thenReturn(400L);
        when(row.getActiveContributors()).thenReturn(3L);
        when(row.getFilesChanged()).thenReturn(5L);

        when(row.getFeatureCommits()).thenReturn(2L);
        when(row.getBugFixCommits()).thenReturn(2L);
        when(row.getRefactorCommits()).thenReturn(1L);
        when(row.getDocumentationCommits()).thenReturn(1L);
        when(row.getTestCommits()).thenReturn(1L);
        when(row.getBuildCommits()).thenReturn(1L);
        when(row.getConfigurationCommits()).thenReturn(0L);
        when(row.getDependencyCommits()).thenReturn(0L);
        when(row.getOtherCommits()).thenReturn(0L);

        when(evolutionJpaRepository.findPeriodSummary(repositoryId, from, to)).thenReturn(row);

        RepositoryEvolutionCompositionResponse response = queryService.getEvolutionComposition(repositoryId, from, to);

        assertThat(response).isNotNull();
        assertThat(response.repositoryId()).isEqualTo(repositoryId);
        assertThat(response.from()).isEqualTo(from);
        assertThat(response.to()).isEqualTo(to);

        // Raw metrics
        assertThat(response.rawMetrics().totalCommits()).isEqualTo(10L);
        assertThat(response.rawMetrics().totalAdditions()).isEqualTo(300L);
        assertThat(response.rawMetrics().totalDeletions()).isEqualTo(100L);
        assertThat(response.rawMetrics().totalChurn()).isEqualTo(400L);
        assertThat(response.rawMetrics().activeContributors()).isEqualTo(3L);
        assertThat(response.rawMetrics().filesChanged()).isEqualTo(5L);
        assertThat(response.rawMetrics().featureCommits()).isEqualTo(2L);
        assertThat(response.rawMetrics().bugFixCommits()).isEqualTo(2L);
        assertThat(response.rawMetrics().refactorCommits()).isEqualTo(1L);
        assertThat(response.rawMetrics().documentationCommits()).isEqualTo(1L);
        assertThat(response.rawMetrics().testCommits()).isEqualTo(1L);
        assertThat(response.rawMetrics().buildCommits()).isEqualTo(1L);
        assertThat(response.rawMetrics().configurationCommits()).isEqualTo(0L);
        assertThat(response.rawMetrics().dependencyCommits()).isEqualTo(0L);
        assertThat(response.rawMetrics().otherCommits()).isEqualTo(0L);

        // Composition
        assertThat(response.composition().classifiedCommits()).isEqualTo(8L);
        assertThat(response.composition().unclassifiedCommits()).isEqualTo(2L); // 10 - 8 = 2
        assertThat(response.composition().featureShare()).isCloseTo(0.25, within(0.0001));       // 2 / 8
        assertThat(response.composition().bugFixShare()).isCloseTo(0.25, within(0.0001));        // 2 / 8
        assertThat(response.composition().refactorShare()).isCloseTo(0.125, within(0.0001));     // 1 / 8
        assertThat(response.composition().documentationShare()).isCloseTo(0.125, within(0.0001));// 1 / 8
        assertThat(response.composition().testShare()).isCloseTo(0.125, within(0.0001));         // 1 / 8
        assertThat(response.composition().buildShare()).isCloseTo(0.125, within(0.0001));        // 1 / 8
        assertThat(response.composition().configurationShare()).isEqualTo(0.0);
        assertThat(response.composition().dependencyShare()).isEqualTo(0.0);
        assertThat(response.composition().otherShare()).isEqualTo(0.0);

        // Intensity
        assertThat(response.intensity().averageChurnPerCommit()).isCloseTo(40.0, within(0.0001));         // 400 / 10
        assertThat(response.intensity().averageFilesChangedPerCommit()).isCloseTo(0.5, within(0.0001));    // 5 / 10
        assertThat(response.intensity().averageAdditionsPerCommit()).isCloseTo(30.0, within(0.0001));      // 300 / 10
        assertThat(response.intensity().averageDeletionsPerCommit()).isCloseTo(10.0, within(0.0001));      // 100 / 10

        verify(repositoryJpaRepository).existsById(repositoryId);
        verify(evolutionJpaRepository).findPeriodSummary(repositoryId, from, to);
    }

    @Test
    @DisplayName("Should return exactly 0.0 for all shares when classifiedCommits is zero")
    void getEvolutionComposition_ZeroClassifiedCommits_ReturnsZeroShares() {
        Long repositoryId = 1L;
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-02-01T00:00:00Z");

        when(repositoryJpaRepository.existsById(repositoryId)).thenReturn(true);

        RepositoryEvolutionPeriodSummaryRow row = mock(RepositoryEvolutionPeriodSummaryRow.class);
        when(row.getTotalCommits()).thenReturn(5L); // 5 unclassified commits, 0 classified
        when(row.getTotalAdditions()).thenReturn(50L);
        when(row.getTotalDeletions()).thenReturn(10L);
        when(row.getTotalChurn()).thenReturn(60L);
        when(row.getActiveContributors()).thenReturn(1L);
        when(row.getFilesChanged()).thenReturn(2L);

        when(row.getFeatureCommits()).thenReturn(0L);
        when(row.getBugFixCommits()).thenReturn(0L);
        when(row.getRefactorCommits()).thenReturn(0L);
        when(row.getDocumentationCommits()).thenReturn(0L);
        when(row.getTestCommits()).thenReturn(0L);
        when(row.getBuildCommits()).thenReturn(0L);
        when(row.getConfigurationCommits()).thenReturn(0L);
        when(row.getDependencyCommits()).thenReturn(0L);
        when(row.getOtherCommits()).thenReturn(0L);

        when(evolutionJpaRepository.findPeriodSummary(repositoryId, from, to)).thenReturn(row);

        RepositoryEvolutionCompositionResponse response = queryService.getEvolutionComposition(repositoryId, from, to);

        assertThat(response.composition().classifiedCommits()).isEqualTo(0L);
        assertThat(response.composition().unclassifiedCommits()).isEqualTo(5L);
        assertThat(response.composition().featureShare()).isEqualTo(0.0);
        assertThat(response.composition().bugFixShare()).isEqualTo(0.0);
        assertThat(response.composition().refactorShare()).isEqualTo(0.0);
        assertThat(response.composition().documentationShare()).isEqualTo(0.0);
        assertThat(response.composition().testShare()).isEqualTo(0.0);
        assertThat(response.composition().buildShare()).isEqualTo(0.0);
        assertThat(response.composition().configurationShare()).isEqualTo(0.0);
        assertThat(response.composition().dependencyShare()).isEqualTo(0.0);
        assertThat(response.composition().otherShare()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should return exactly 0.0 for all intensity and share metrics when totalCommits is zero")
    void getEvolutionComposition_ZeroTotalCommits_ReturnsZeroMetrics() {
        Long repositoryId = 1L;
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-02-01T00:00:00Z");

        when(repositoryJpaRepository.existsById(repositoryId)).thenReturn(true);

        RepositoryEvolutionPeriodSummaryRow row = mock(RepositoryEvolutionPeriodSummaryRow.class);
        when(row.getTotalCommits()).thenReturn(0L);
        when(row.getTotalAdditions()).thenReturn(0L);
        when(row.getTotalDeletions()).thenReturn(0L);
        when(row.getTotalChurn()).thenReturn(0L);
        when(row.getActiveContributors()).thenReturn(0L);
        when(row.getFilesChanged()).thenReturn(0L);

        when(evolutionJpaRepository.findPeriodSummary(repositoryId, from, to)).thenReturn(row);

        RepositoryEvolutionCompositionResponse response = queryService.getEvolutionComposition(repositoryId, from, to);

        assertThat(response.composition().classifiedCommits()).isEqualTo(0L);
        assertThat(response.composition().unclassifiedCommits()).isEqualTo(0L);
        assertThat(response.composition().featureShare()).isEqualTo(0.0);

        assertThat(response.intensity().averageChurnPerCommit()).isEqualTo(0.0);
        assertThat(response.intensity().averageFilesChangedPerCommit()).isEqualTo(0.0);
        assertThat(response.intensity().averageAdditionsPerCommit()).isEqualTo(0.0);
        assertThat(response.intensity().averageDeletionsPerCommit()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when from is null")
    void getEvolutionComposition_NullFrom_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> queryService.getEvolutionComposition(1L, null, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The 'from' timestamp parameter is required");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when to is null")
    void getEvolutionComposition_NullTo_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> queryService.getEvolutionComposition(1L, Instant.now(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The 'to' timestamp parameter is required");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when from is not strictly before to")
    void getEvolutionComposition_InvalidDateRange_ThrowsIllegalArgumentException() {
        Instant now = Instant.now();
        assertThatThrownBy(() -> queryService.getEvolutionComposition(1L, now, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The 'from' timestamp must be strictly before the 'to' timestamp");

        assertThatThrownBy(() -> queryService.getEvolutionComposition(1L, now, now.minusSeconds(3600)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The 'from' timestamp must be strictly before the 'to' timestamp");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when repository does not exist")
    void getEvolutionComposition_RepoNotFound_ThrowsResourceNotFoundException() {
        Long repositoryId = 999L;
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-02-01T00:00:00Z");

        when(repositoryJpaRepository.existsById(repositoryId)).thenReturn(false);

        assertThatThrownBy(() -> queryService.getEvolutionComposition(repositoryId, from, to))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Repository not found with id: 999");
    }
}
