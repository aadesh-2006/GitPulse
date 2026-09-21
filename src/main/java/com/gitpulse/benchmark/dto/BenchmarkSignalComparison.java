package com.gitpulse.benchmark.dto;

/**
 * Direct comparison summary between Baseline (revision frequency only) and Composite multidimensional hotspot score.
 */
public record BenchmarkSignalComparison(
        double baselinePrecisionAt10,
        double compositePrecisionAt10,
        double precisionDeltaAt10,
        double baselineRecallAt10,
        double compositeRecallAt10,
        double recallDeltaAt10,
        double baselineSpearmanRevisions,
        double compositeSpearmanRevisions,
        double spearmanRevisionsDelta,
        double baselineRocAuc,
        double compositeRocAuc,
        double rocAucDelta
) {
    public static BenchmarkSignalComparison of(BenchmarkRankingMetrics baseline, BenchmarkRankingMetrics composite) {
        return new BenchmarkSignalComparison(
                baseline.precisionAt10(),
                composite.precisionAt10(),
                composite.precisionAt10() - baseline.precisionAt10(),
                baseline.recallAt10(),
                composite.recallAt10(),
                composite.recallAt10() - baseline.recallAt10(),
                baseline.spearmanCorrelationRevisions(),
                composite.spearmanCorrelationRevisions(),
                composite.spearmanCorrelationRevisions() - baseline.spearmanCorrelationRevisions(),
                baseline.rocAreaUnderCurve(),
                composite.rocAreaUnderCurve(),
                composite.rocAreaUnderCurve() - baseline.rocAreaUnderCurve()
        );
    }
}
