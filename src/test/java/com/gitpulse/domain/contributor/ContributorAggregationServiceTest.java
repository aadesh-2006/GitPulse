package com.gitpulse.domain.contributor;

import com.gitpulse.common.exception.ResourceNotFoundException;
import com.gitpulse.domain.contributor.dto.ContributorAggregationResult;
import com.gitpulse.domain.contributor.dto.ContributorAggregationRow;
import com.gitpulse.domain.repository.Repository;
import com.gitpulse.domain.repository.RepositoryJpaRepository;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContributorAggregationServiceTest {

    @Mock
    private RepositoryJpaRepository repositoryJpaRepository;

    @Mock
    private ContributorJpaRepository contributorJpaRepository;

    @Mock
    private RepositoryContributorJpaRepository repositoryContributorJpaRepository;

    @InjectMocks
    private ContributorAggregationService contributorAggregationService;

    private Repository testRepository;

    @BeforeEach
    void setUp() {
        testRepository = new Repository("octocat", "Hello-World", "Sample repo", "main");
        ReflectionTestUtils.setField(testRepository, "id", 1L);
    }

    @Test
    @DisplayName("Should create new contributors and attributions on first analysis")
    void aggregateNewContributors() {
        when(repositoryJpaRepository.findById(1L)).thenReturn(Optional.of(testRepository));

        Instant t1 = Instant.parse("2026-09-01T10:00:00Z");
        Instant t2 = Instant.parse("2026-09-05T12:00:00Z");

        ContributorAggregationRow row1 = createAggregationRow("alice@test.com", "alice", "Alice", 10, 100, 20, 120, t1, t2);
        ContributorAggregationRow row2 = createAggregationRow("bob@test.com", "bob", "Bob", 5, 50, 10, 60, t1, t2);

        when(repositoryContributorJpaRepository.aggregateContributorsByRepositoryId(1L))
                .thenReturn(List.of(row1, row2));
        when(contributorJpaRepository.findByEmailIn(Set.of("alice@test.com", "bob@test.com")))
                .thenReturn(Collections.emptyList());
        when(repositoryContributorJpaRepository.findByRepositoryId(1L))
                .thenReturn(Collections.emptyList());

        ContributorAggregationResult result = contributorAggregationService.aggregateContributors(1L, 100L);

        assertThat(result.getContributorsAggregated()).isEqualTo(2);
        assertThat(result.getContributorsCreated()).isEqualTo(2);
        assertThat(result.getAttributionsCreated()).isEqualTo(2);
        assertThat(result.getAttributionsUpdated()).isEqualTo(0);
        assertThat(result.getDurationMs()).isGreaterThanOrEqualTo(0);

        verify(contributorJpaRepository, times(1)).saveAll(any());
        verify(repositoryContributorJpaRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("Re-analysis: should update existing metrics and not double-count")
    void aggregateExistingContributorsWithoutDoubleCounting() {
        when(repositoryJpaRepository.findById(1L)).thenReturn(Optional.of(testRepository));

        Instant t1 = Instant.parse("2026-09-01T10:00:00Z");
        Instant t2 = Instant.parse("2026-09-10T12:00:00Z");

        Contributor existingContributor = new Contributor("alice@test.com", "alice", "Alice");
        ReflectionTestUtils.setField(existingContributor, "id", 10L);

        RepositoryContributor existingAttribution = new RepositoryContributor(
                testRepository, existingContributor, 5, 50, 10, 60, t1, t1
        );
        ReflectionTestUtils.setField(existingAttribution, "id", 100L);

        // New aggregation has cumulative total of 15 commits (not +15, but total 15)
        ContributorAggregationRow row = createAggregationRow("alice@test.com", "alice_new", "Alice Updated", 15, 150, 30, 180, t1, t2);

        when(repositoryContributorJpaRepository.aggregateContributorsByRepositoryId(1L))
                .thenReturn(List.of(row));
        when(contributorJpaRepository.findByEmailIn(Set.of("alice@test.com")))
                .thenReturn(List.of(existingContributor));
        when(repositoryContributorJpaRepository.findByRepositoryId(1L))
                .thenReturn(List.of(existingAttribution));

        ContributorAggregationResult result = contributorAggregationService.aggregateContributors(1L, 100L);

        assertThat(result.getContributorsAggregated()).isEqualTo(1);
        assertThat(result.getContributorsCreated()).isEqualTo(0);
        assertThat(result.getAttributionsCreated()).isEqualTo(0);
        assertThat(result.getAttributionsUpdated()).isEqualTo(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RepositoryContributor>> captor = ArgumentCaptor.forClass(List.class);
        verify(repositoryContributorJpaRepository).saveAll(captor.capture());

        List<RepositoryContributor> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getTotalCommits()).isEqualTo(15);
        assertThat(saved.get(0).getTotalChanges()).isEqualTo(180);
        assertThat(saved.get(0).getLastCommittedAt()).isEqualTo(t2);

        assertThat(existingContributor.getUsername()).isEqualTo("alice_new");
        assertThat(existingContributor.getName()).isEqualTo("Alice Updated");
    }

    @Test
    @DisplayName("Should handle empty aggregation gracefully when repository has no commits")
    void handleEmptyAggregation() {
        when(repositoryJpaRepository.findById(1L)).thenReturn(Optional.of(testRepository));
        when(repositoryContributorJpaRepository.aggregateContributorsByRepositoryId(1L))
                .thenReturn(Collections.emptyList());

        ContributorAggregationResult result = contributorAggregationService.aggregateContributors(1L, 100L);

        assertThat(result.getContributorsAggregated()).isEqualTo(0);
        assertThat(result.getContributorsCreated()).isEqualTo(0);
        assertThat(result.getAttributionsCreated()).isEqualTo(0);
        assertThat(result.getAttributionsUpdated()).isEqualTo(0);

        verify(contributorJpaRepository, never()).saveAll(any());
        verify(repositoryContributorJpaRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when repository does not exist")
    void repositoryNotFound() {
        when(repositoryJpaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contributorAggregationService.aggregateContributors(999L, 100L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repositoryContributorJpaRepository, never()).aggregateContributorsByRepositoryId(any());
    }

    private ContributorAggregationRow createAggregationRow(String email,
                                                           String username,
                                                           String name,
                                                           int totalCommits,
                                                           int totalAdditions,
                                                           int totalDeletions,
                                                           int totalChanges,
                                                           Instant firstCommittedAt,
                                                           Instant lastCommittedAt) {
        return new ContributorAggregationRow() {
            @Override public String getEmail() { return email; }
            @Override public String getUsername() { return username; }
            @Override public String getName() { return name; }
            @Override public int getTotalCommits() { return totalCommits; }
            @Override public int getTotalAdditions() { return totalAdditions; }
            @Override public int getTotalDeletions() { return totalDeletions; }
            @Override public int getTotalChanges() { return totalChanges; }
            @Override public java.time.OffsetDateTime getFirstCommittedAt() {
                return firstCommittedAt != null ? firstCommittedAt.atOffset(java.time.ZoneOffset.UTC) : null;
            }
            @Override public java.time.OffsetDateTime getLastCommittedAt() {
                return lastCommittedAt != null ? lastCommittedAt.atOffset(java.time.ZoneOffset.UTC) : null;
            }
        };
    }
}
