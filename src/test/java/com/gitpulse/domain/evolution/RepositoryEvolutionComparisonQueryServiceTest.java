package com.gitpulse.domain.evolution;

import com.gitpulse.common.exception.ResourceNotFoundException;
import com.gitpulse.domain.evolution.dto.RepositoryEvolutionComparisonResponse;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryEvolutionComparisonQueryServiceTest {

    @Mock
    private RepositoryJpaRepository repositoryJpaRepository;

    @Mock
    private RepositoryEvolutionJpaRepository evolutionJpaRepository;

    private RepositoryEvolutionComparisonQueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new RepositoryEvolutionComparisonQueryService(repositoryJpaRepository, evolutionJpaRepository);
    }

    @Test
    @DisplayName("Should successfully compare two periods and calculate signed deltas correctly")
    void compareRepositoryEvolution_Success() {
        Long repositoryId = 1L;
        Instant currentFrom = Instant.parse("2026-02-01T00:00:00Z");
        Instant currentTo = Instant.parse("2026-03-01T00:00:00Z");
        Instant previousFrom = Instant.parse("2026-01-01T00:00:00Z");
        Instant previousTo = Instant.parse("2026-02-01T00:00:00Z");

        when(repositoryJpaRepository.existsById(repositoryId)).thenReturn(true);

        RepositoryEvolutionPeriodSummaryRow currentRow = mock(RepositoryEvolutionPeriodSummaryRow.class);
        when(currentRow.getTotalCommits()).thenReturn(10L);
        when(currentRow.getTotalAdditions()).thenReturn(250L);
        when(currentRow.getTotalDeletions()).thenReturn(50L);
        when(currentRow.getTotalChurn()).thenReturn(300L);
        when(currentRow.getActiveContributors()).thenReturn(3L);
        when(currentRow.getFilesChanged()).thenReturn(8L);
        when(currentRow.getFeatureCommits()).thenReturn(5L);
        when(currentRow.getBugFixCommits()).thenReturn(2L);
        when(currentRow.getRefactorCommits()).thenReturn(1L);
        when(currentRow.getDocumentationCommits()).thenReturn(1L);
        when(currentRow.getTestCommits()).thenReturn(1L);
        when(currentRow.getBuildCommits()).thenReturn(0L);
        when(currentRow.getConfigurationCommits()).thenReturn(0L);
        when(currentRow.getDependencyCommits()).thenReturn(0L);
        when(currentRow.getOtherCommits()).thenReturn(0L);

        RepositoryEvolutionPeriodSummaryRow previousRow = mock(RepositoryEvolutionPeriodSummaryRow.class);
        when(previousRow.getTotalCommits()).thenReturn(6L);
        when(previousRow.getTotalAdditions()).thenReturn(300L);
        when(previousRow.getTotalDeletions()).thenReturn(100L);
        when(previousRow.getTotalChurn()).thenReturn(400L);
        when(previousRow.getActiveContributors()).thenReturn(5L);
        when(previousRow.getFilesChanged()).thenReturn(10L);
        when(previousRow.getFeatureCommits()).thenReturn(2L);
        when(previousRow.getBugFixCommits()).thenReturn(3L);
        when(previousRow.getRefactorCommits()).thenReturn(1L);
        when(previousRow.getDocumentationCommits()).thenReturn(0L);
        when(previousRow.getTestCommits()).thenReturn(0L);
        when(previousRow.getBuildCommits()).thenReturn(0L);
        when(previousRow.getConfigurationCommits()).thenReturn(0L);
        when(previousRow.getDependencyCommits()).thenReturn(0L);
        when(previousRow.getOtherCommits()).thenReturn(0L);

        when(evolutionJpaRepository.findPeriodSummary(repositoryId, currentFrom, currentTo)).thenReturn(currentRow);
        when(evolutionJpaRepository.findPeriodSummary(repositoryId, previousFrom, previousTo)).thenReturn(previousRow);

        RepositoryEvolutionComparisonResponse response = queryService.compareRepositoryEvolution(
                repositoryId, currentFrom, currentTo, previousFrom, previousTo
        );

        assertThat(response).isNotNull();
        assertThat(response.repositoryId()).isEqualTo(repositoryId);

        // Current Period
        assertThat(response.currentPeriod().from()).isEqualTo(currentFrom);
        assertThat(response.currentPeriod().to()).isEqualTo(currentTo);
        assertThat(response.currentPeriod().totalCommits()).isEqualTo(10L);
        assertThat(response.currentPeriod().totalAdditions()).isEqualTo(250L);
        assertThat(response.currentPeriod().totalDeletions()).isEqualTo(50L);
        assertThat(response.currentPeriod().totalChurn()).isEqualTo(300L);
        assertThat(response.currentPeriod().activeContributors()).isEqualTo(3L);
        assertThat(response.currentPeriod().filesChanged()).isEqualTo(8L);
        assertThat(response.currentPeriod().featureCommits()).isEqualTo(5L);
        assertThat(response.currentPeriod().bugFixCommits()).isEqualTo(2L);

        // Previous Period
        assertThat(response.previousPeriod().from()).isEqualTo(previousFrom);
        assertThat(response.previousPeriod().to()).isEqualTo(previousTo);
        assertThat(response.previousPeriod().totalCommits()).isEqualTo(6L);
        assertThat(response.previousPeriod().totalAdditions()).isEqualTo(300L);
        assertThat(response.previousPeriod().totalDeletions()).isEqualTo(100L);
        assertThat(response.previousPeriod().totalChurn()).isEqualTo(400L);
        assertThat(response.previousPeriod().activeContributors()).isEqualTo(5L);
        assertThat(response.previousPeriod().filesChanged()).isEqualTo(10L);
        assertThat(response.previousPeriod().featureCommits()).isEqualTo(2L);
        assertThat(response.previousPeriod().bugFixCommits()).isEqualTo(3L);

        // Delta (current - previous)
        assertThat(response.delta().totalCommits()).isEqualTo(4L);      // 10 - 6 = +4
        assertThat(response.delta().totalAdditions()).isEqualTo(-50L);   // 250 - 300 = -50
        assertThat(response.delta().totalDeletions()).isEqualTo(-50L);   // 50 - 100 = -50
        assertThat(response.delta().totalChurn()).isEqualTo(-100L);     // 300 - 400 = -100
        assertThat(response.delta().activeContributors()).isEqualTo(-2L); // 3 - 5 = -2
        assertThat(response.delta().filesChanged()).isEqualTo(-2L);     // 8 - 10 = -2
        assertThat(response.delta().featureCommits()).isEqualTo(3L);    // 5 - 2 = +3
        assertThat(response.delta().bugFixCommits()).isEqualTo(-1L);    // 2 - 3 = -1
        assertThat(response.delta().refactorCommits()).isEqualTo(0L);   // 1 - 1 = 0

        verify(repositoryJpaRepository).existsById(repositoryId);
        verify(evolutionJpaRepository).findPeriodSummary(repositoryId, currentFrom, currentTo);
        verify(evolutionJpaRepository).findPeriodSummary(repositoryId, previousFrom, previousTo);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when currentFrom is null")
    void compareRepositoryEvolution_NullCurrentFrom_ThrowsIllegalArgumentException() {
        Instant now = Instant.now();
        assertThatThrownBy(() -> queryService.compareRepositoryEvolution(1L, null, now, now.minusSeconds(3600), now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The 'currentFrom' timestamp parameter is required");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when currentTo is null")
    void compareRepositoryEvolution_NullCurrentTo_ThrowsIllegalArgumentException() {
        Instant now = Instant.now();
        assertThatThrownBy(() -> queryService.compareRepositoryEvolution(1L, now.minusSeconds(3600), null, now.minusSeconds(3600), now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The 'currentTo' timestamp parameter is required");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when previousFrom is null")
    void compareRepositoryEvolution_NullPreviousFrom_ThrowsIllegalArgumentException() {
        Instant now = Instant.now();
        assertThatThrownBy(() -> queryService.compareRepositoryEvolution(1L, now.minusSeconds(3600), now, null, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The 'previousFrom' timestamp parameter is required");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when previousTo is null")
    void compareRepositoryEvolution_NullPreviousTo_ThrowsIllegalArgumentException() {
        Instant now = Instant.now();
        assertThatThrownBy(() -> queryService.compareRepositoryEvolution(1L, now.minusSeconds(3600), now, now.minusSeconds(7200), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The 'previousTo' timestamp parameter is required");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when currentFrom >= currentTo")
    void compareRepositoryEvolution_InvalidCurrentRange_ThrowsIllegalArgumentException() {
        Instant now = Instant.now();
        assertThatThrownBy(() -> queryService.compareRepositoryEvolution(1L, now, now, now.minusSeconds(7200), now.minusSeconds(3600)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The 'currentFrom' timestamp must be strictly before the 'currentTo' timestamp");

        assertThatThrownBy(() -> queryService.compareRepositoryEvolution(1L, now, now.minusSeconds(3600), now.minusSeconds(7200), now.minusSeconds(3600)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The 'currentFrom' timestamp must be strictly before the 'currentTo' timestamp");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when previousFrom >= previousTo")
    void compareRepositoryEvolution_InvalidPreviousRange_ThrowsIllegalArgumentException() {
        Instant now = Instant.now();
        Instant currentFrom = now.minusSeconds(3600);
        Instant currentTo = now;

        assertThatThrownBy(() -> queryService.compareRepositoryEvolution(1L, currentFrom, currentTo, now, now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The 'previousFrom' timestamp must be strictly before the 'previousTo' timestamp");

        assertThatThrownBy(() -> queryService.compareRepositoryEvolution(1L, currentFrom, currentTo, now, now.minusSeconds(3600)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The 'previousFrom' timestamp must be strictly before the 'previousTo' timestamp");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when repository does not exist")
    void compareRepositoryEvolution_RepoNotFound_ThrowsResourceNotFoundException() {
        Long repositoryId = 999L;
        Instant currentFrom = Instant.parse("2026-02-01T00:00:00Z");
        Instant currentTo = Instant.parse("2026-03-01T00:00:00Z");
        Instant previousFrom = Instant.parse("2026-01-01T00:00:00Z");
        Instant previousTo = Instant.parse("2026-02-01T00:00:00Z");

        when(repositoryJpaRepository.existsById(repositoryId)).thenReturn(false);

        assertThatThrownBy(() -> queryService.compareRepositoryEvolution(
                repositoryId, currentFrom, currentTo, previousFrom, previousTo
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Repository not found with id: 999");
    }
}
