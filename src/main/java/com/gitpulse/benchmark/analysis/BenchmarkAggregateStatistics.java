package com.gitpulse.benchmark.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.ToDoubleFunction;

/**
 * Statistical aggregation utilities for cross-repository benchmark evaluations.
 */
public final class BenchmarkAggregateStatistics {

    private BenchmarkAggregateStatistics() {
        // Utility class
    }

    public record DistributionSummary(
            double mean,
            double median,
            double standardDeviation,
            double min,
            double max,
            int count
    ) {
        public static DistributionSummary empty() {
            return new DistributionSummary(0.0, 0.0, 0.0, 0.0, 0.0, 0);
        }
    }

    public record PairedComparisonSummary(
            DistributionSummary baselineDistribution,
            DistributionSummary compositeDistribution,
            DistributionSummary deltaDistribution,
            int compositeHigherCount,
            int baselineHigherCount,
            int tieCount
    ) {
    }

    /**
     * Calculates distribution metrics (mean, median, standard deviation, min, max) for a list of values.
     */
    public static DistributionSummary calculateDistribution(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return DistributionSummary.empty();
        }

        List<Double> validValues = values.stream()
                .filter(Objects::nonNull)
                .filter(v -> !Double.isNaN(v) && !Double.isInfinite(v))
                .toList();

        int n = validValues.size();
        if (n == 0) {
            return DistributionSummary.empty();
        }

        double sum = 0.0;
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;

        for (double v : validValues) {
            sum += v;
            if (v < min) min = v;
            if (v > max) max = v;
        }

        double mean = sum / n;

        double sumSqDiff = 0.0;
        for (double v : validValues) {
            double diff = v - mean;
            sumSqDiff += diff * diff;
        }
        double stdDev = n > 1 ? Math.sqrt(sumSqDiff / (n - 1)) : 0.0;

        List<Double> sorted = new ArrayList<>(validValues);
        Collections.sort(sorted);
        double median;
        if (n % 2 == 1) {
            median = sorted.get(n / 2);
        } else {
            median = (sorted.get((n / 2) - 1) + sorted.get(n / 2)) / 2.0;
        }

        return new DistributionSummary(mean, median, stdDev, min, max, n);
    }

    /**
     * Calculates paired differences and distribution summaries between baseline and composite metrics.
     */
    public static <T> PairedComparisonSummary comparePaired(
            List<T> runs,
            ToDoubleFunction<T> baselineExtractor,
            ToDoubleFunction<T> compositeExtractor
    ) {
        Objects.requireNonNull(runs, "runs must not be null");
        Objects.requireNonNull(baselineExtractor, "baselineExtractor must not be null");
        Objects.requireNonNull(compositeExtractor, "compositeExtractor must not be null");

        List<Double> baselineValues = new ArrayList<>(runs.size());
        List<Double> compositeValues = new ArrayList<>(runs.size());
        List<Double> deltaValues = new ArrayList<>(runs.size());

        int compositeHigher = 0;
        int baselineHigher = 0;
        int ties = 0;

        for (T run : runs) {
            double b = baselineExtractor.applyAsDouble(run);
            double c = compositeExtractor.applyAsDouble(run);

            if (Double.isNaN(b) || Double.isInfinite(b) || Double.isNaN(c) || Double.isInfinite(c)) {
                continue;
            }

            baselineValues.add(b);
            compositeValues.add(c);
            double delta = c - b;
            deltaValues.add(delta);

            if (Math.abs(delta) < 1e-9) {
                ties++;
            } else if (delta > 0) {
                compositeHigher++;
            } else {
                baselineHigher++;
            }
        }

        DistributionSummary bDist = calculateDistribution(baselineValues);
        DistributionSummary cDist = calculateDistribution(compositeValues);
        DistributionSummary dDist = calculateDistribution(deltaValues);

        return new PairedComparisonSummary(
                bDist,
                cDist,
                dDist,
                compositeHigher,
                baselineHigher,
                ties
        );
    }
}
