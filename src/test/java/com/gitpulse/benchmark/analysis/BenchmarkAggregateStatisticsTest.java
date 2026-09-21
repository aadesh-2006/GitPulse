package com.gitpulse.benchmark.analysis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class BenchmarkAggregateStatisticsTest {

    private record TestRun(double baseline, double composite) {}

    @Test
    @DisplayName("Should compute accurate distribution metrics (mean, median, std, min, max)")
    void shouldComputeAccurateDistribution() {
        List<Double> values = List.of(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0);
        BenchmarkAggregateStatistics.DistributionSummary summary = BenchmarkAggregateStatistics.calculateDistribution(values);

        assertThat(summary.count()).isEqualTo(8);
        assertThat(summary.mean()).isCloseTo(5.0, within(1e-9));
        assertThat(summary.median()).isCloseTo(4.5, within(1e-9));
        assertThat(summary.min()).isEqualTo(2.0);
        assertThat(summary.max()).isEqualTo(9.0);
        // sample std dev of [2, 4, 4, 4, 5, 5, 7, 9] = sqrt(32 / 7) ≈ 2.1380899
        assertThat(summary.standardDeviation()).isCloseTo(2.1380899, within(1e-5));
    }

    @Test
    @DisplayName("Should handle empty and single-element lists safely")
    void shouldHandleEmptyAndSingle() {
        BenchmarkAggregateStatistics.DistributionSummary empty = BenchmarkAggregateStatistics.calculateDistribution(Collections.emptyList());
        assertThat(empty.count()).isEqualTo(0);
        assertThat(empty.mean()).isEqualTo(0.0);

        BenchmarkAggregateStatistics.DistributionSummary single = BenchmarkAggregateStatistics.calculateDistribution(List.of(10.0));
        assertThat(single.count()).isEqualTo(1);
        assertThat(single.mean()).isEqualTo(10.0);
        assertThat(single.median()).isEqualTo(10.0);
        assertThat(single.standardDeviation()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should compute paired comparison counts (composite higher, baseline higher, ties)")
    void shouldComputePairedCounts() {
        List<TestRun> runs = List.of(
                new TestRun(0.50, 0.60), // composite higher (+0.10)
                new TestRun(0.40, 0.40), // tie (0.00)
                new TestRun(0.70, 0.50), // baseline higher (-0.20)
                new TestRun(0.30, 0.45)  // composite higher (+0.15)
        );

        BenchmarkAggregateStatistics.PairedComparisonSummary summary = BenchmarkAggregateStatistics.comparePaired(
                runs,
                TestRun::baseline,
                TestRun::composite
        );

        assertThat(summary.compositeHigherCount()).isEqualTo(2);
        assertThat(summary.baselineHigherCount()).isEqualTo(1);
        assertThat(summary.tieCount()).isEqualTo(1);

        assertThat(summary.baselineDistribution().mean()).isCloseTo(0.475, within(1e-9));
        assertThat(summary.compositeDistribution().mean()).isCloseTo(0.4875, within(1e-9));
        assertThat(summary.deltaDistribution().mean()).isCloseTo(0.0125, within(1e-9));
    }
}
