package com.gitpulse.benchmark.dto;

/**
 * Immutable record representing the observed activity on a file during the future evaluation window
 * (T_cutoff, T_cutoff + horizon].
 */
public record BenchmarkFutureOutcome(
        String filePath,
        long futureRevisionCount,
        long futureChurn,
        long futureDistinctContributors,
        boolean futureChanged
) {
    public static BenchmarkFutureOutcome empty(String filePath) {
        return new BenchmarkFutureOutcome(filePath, 0L, 0L, 0L, false);
    }
}
