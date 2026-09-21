package com.gitpulse.benchmark.analysis;

import com.gitpulse.benchmark.dto.BenchmarkResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Subgroup classification and robustness analysis for repository benchmark runs.
 */
public final class BenchmarkSubgroupAnalyzer {

    private BenchmarkSubgroupAnalyzer() {
        // Utility class
    }

    public enum SizeSubgroup {
        SMALL,
        MEDIUM,
        LARGE
    }

    public enum ActivitySubgroup {
        LOWER_ACTIVITY,
        HIGHER_ACTIVITY
    }

    /**
     * Categorizes a benchmark result by repository size based strictly on historical files at cutoff.
     */
    public static SizeSubgroup classifySize(int historicalFileCount) {
        if (historicalFileCount < 50) {
            return SizeSubgroup.SMALL;
        } else if (historicalFileCount <= 500) {
            return SizeSubgroup.MEDIUM;
        } else {
            return SizeSubgroup.LARGE;
        }
    }

    /**
     * Groups benchmark results by size subgroup and computes paired comparisons for Precision@10.
     */
    public static Map<SizeSubgroup, BenchmarkAggregateStatistics.PairedComparisonSummary> analyzeBySize(
            List<BenchmarkResult> results
    ) {
        Objects.requireNonNull(results, "results must not be null");

        Map<SizeSubgroup, List<BenchmarkResult>> grouped = results.stream()
                .collect(Collectors.groupingBy(r -> classifySize(r.totalHistoricalFiles())));

        Map<SizeSubgroup, BenchmarkAggregateStatistics.PairedComparisonSummary> summary = new LinkedHashMap<>();
        for (SizeSubgroup subgroup : SizeSubgroup.values()) {
            List<BenchmarkResult> subgroupRuns = grouped.getOrDefault(subgroup, List.of());
            summary.put(
                    subgroup,
                    BenchmarkAggregateStatistics.comparePaired(
                            subgroupRuns,
                            r -> r.baselineMetrics().precisionAt10(),
                            r -> r.compositeMetrics().precisionAt10()
                    )
            );
        }

        return summary;
    }

    /**
     * Categorizes a benchmark result by activity level based on future active files relative to historical files.
     */
    public static ActivitySubgroup classifyActivity(int futureActiveFiles, int totalHistoricalFiles) {
        if (totalHistoricalFiles <= 0) {
            return ActivitySubgroup.LOWER_ACTIVITY;
        }
        double activityRatio = (double) futureActiveFiles / totalHistoricalFiles;
        return activityRatio >= 0.20 ? ActivitySubgroup.HIGHER_ACTIVITY : ActivitySubgroup.LOWER_ACTIVITY;
    }

    /**
     * Groups benchmark results by activity subgroup and computes paired comparisons for Precision@10.
     */
    public static Map<ActivitySubgroup, BenchmarkAggregateStatistics.PairedComparisonSummary> analyzeByActivity(
            List<BenchmarkResult> results
    ) {
        Objects.requireNonNull(results, "results must not be null");

        Map<ActivitySubgroup, List<BenchmarkResult>> grouped = results.stream()
                .collect(Collectors.groupingBy(r -> classifyActivity(r.totalActiveFutureFiles(), r.totalHistoricalFiles())));

        Map<ActivitySubgroup, BenchmarkAggregateStatistics.PairedComparisonSummary> summary = new LinkedHashMap<>();
        for (ActivitySubgroup subgroup : ActivitySubgroup.values()) {
            List<BenchmarkResult> subgroupRuns = grouped.getOrDefault(subgroup, List.of());
            summary.put(
                    subgroup,
                    BenchmarkAggregateStatistics.comparePaired(
                            subgroupRuns,
                            r -> r.baselineMetrics().precisionAt10(),
                            r -> r.compositeMetrics().precisionAt10()
                    )
            );
        }

        return summary;
    }
}
