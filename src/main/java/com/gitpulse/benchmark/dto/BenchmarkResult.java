package com.gitpulse.benchmark.dto;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Complete evaluation result of a temporal benchmark run on a repository.
 */
public record BenchmarkResult(
        Long repositoryId,
        String repositoryName,
        Instant cutoffTime,
        Duration horizonDuration,
        int totalHistoricalFiles,
        int totalActiveFutureFiles,
        int futureChangedHistoricalFiles,
        int newFilesIntroducedInFuture,
        BenchmarkRankingMetrics baselineMetrics,
        BenchmarkRankingMetrics compositeMetrics,
        Map<String, BenchmarkRankingMetrics> ablationMetrics,
        BenchmarkSignalComparison comparisonSummary,
        long executionDurationMs
) {
}
