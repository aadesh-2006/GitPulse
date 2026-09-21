package com.gitpulse.benchmark.analysis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BenchmarkCutoffSelectorTest {

    private final Instant start = Instant.parse("2023-01-01T00:00:00Z");
    private final Instant end = Instant.parse("2025-01-01T00:00:00Z");
    private final Duration horizon = Duration.ofDays(90);

    @Test
    @DisplayName("Should generate 3 evenly spaced deterministic cutoffs with valid bounds")
    void shouldGenerate3Cutoffs() {
        List<Instant> cutoffs = BenchmarkCutoffSelector.selectCutoffs(start, end, horizon, 3);

        assertThat(cutoffs).hasSize(3);
        // First cutoff must be >= start + horizon
        assertThat(cutoffs.get(0)).isAfterOrEqualTo(start.plus(horizon));
        // Last cutoff + horizon must be <= end
        assertThat(cutoffs.get(2).plus(horizon)).isBeforeOrEqualTo(end);
        // Cutoffs must be strictly increasing
        assertThat(cutoffs.get(0)).isBefore(cutoffs.get(1));
        assertThat(cutoffs.get(1)).isBefore(cutoffs.get(2));
    }

    @Test
    @DisplayName("Should handle 1 cutoff by choosing exact interval midpoint")
    void shouldHandleSingleCutoff() {
        List<Instant> cutoffs = BenchmarkCutoffSelector.selectCutoffs(start, end, horizon, 1);

        assertThat(cutoffs).hasSize(1);
        Instant minValid = start.plus(horizon);
        Instant maxValid = end.minus(horizon);
        Instant expectedMidpoint = Instant.ofEpochSecond((minValid.getEpochSecond() + maxValid.getEpochSecond()) / 2);
        assertThat(cutoffs.get(0)).isEqualTo(expectedMidpoint);
    }

    @Test
    @DisplayName("Should return empty list if repository history is shorter than 2x horizon")
    void shouldReturnEmptyForInsufficientHistory() {
        Instant shortEnd = start.plus(Duration.ofDays(100)); // less than 2x 90 days = 180 days
        List<Instant> cutoffs = BenchmarkCutoffSelector.selectCutoffs(start, shortEnd, horizon, 3);
        assertThat(cutoffs).isEmpty();
    }
}
