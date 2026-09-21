package com.gitpulse.benchmark.dto;

/**
 * Immutable evaluation metrics calculated for a single risk signal.
 */
public record BenchmarkRankingMetrics(
        String signalName,
        double precisionAt5,
        double precisionAt10,
        double precisionAt20,
        double recallAt5,
        double recallAt10,
        double recallAt20,
        double hitRateAt5,
        double hitRateAt10,
        double hitRateAt20,
        double spearmanCorrelationRevisions,
        double spearmanCorrelationChurn,
        double rocAreaUnderCurve
) {
}
