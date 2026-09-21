package com.gitpulse.benchmark.metric;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.ToDoubleFunction;

/**
 * Deterministic, pure mathematical calculation engine for repository risk benchmarking metrics.
 * <p>
 * Provides statistical ranking and evaluation measures:
 * <ul>
 *   <li>Precision@K: Fraction of top K ranked items with positive target outcomes</li>
 *   <li>Recall@K: Fraction of all positive target outcomes present in top K ranked items</li>
 *   <li>HitRate@K: Indicator whether at least one positive item is captured in top K</li>
 *   <li>Spearman Rank Correlation: Pearson correlation of fractional ranks (with tie averaging)</li>
 *   <li>ROC-AUC: Area Under the Receiver Operating Characteristic curve via Mann-Whitney U statistic</li>
 * </ul>
 */
public final class BenchmarkMetricsCalculator {

    private BenchmarkMetricsCalculator() {
        // Utility class
    }

    /**
     * Calculates Precision@K for a list of items ordered descending by score.
     *
     * @param rankedItems   items pre-sorted by rank score descending, must not be null
     * @param isPositive    predicate function determining positive outcome
     * @param k             top-K cutoff threshold (must be &gt; 0)
     * @param <T>           item type
     * @return precision in [0.0, 1.0]
     */
    public static <T> double precisionAtK(List<T> rankedItems, java.util.function.Predicate<T> isPositive, int k) {
        Objects.requireNonNull(rankedItems, "rankedItems must not be null");
        Objects.requireNonNull(isPositive, "isPositive predicate must not be null");
        if (k <= 0 || rankedItems.isEmpty()) {
            return 0.0;
        }

        int limit = Math.min(k, rankedItems.size());
        long positiveCount = 0;
        for (int i = 0; i < limit; i++) {
            if (isPositive.test(rankedItems.get(i))) {
                positiveCount++;
            }
        }

        return (double) positiveCount / limit;
    }

    /**
     * Calculates Recall@K for a list of items ordered descending by score.
     *
     * @param rankedItems   items pre-sorted by rank score descending, must not be null
     * @param isPositive    predicate function determining positive outcome
     * @param totalPositives total number of positive items in the entire evaluation population
     * @param k             top-K cutoff threshold (must be &gt; 0)
     * @param <T>           item type
     * @return recall in [0.0, 1.0]
     */
    public static <T> double recallAtK(List<T> rankedItems, java.util.function.Predicate<T> isPositive, long totalPositives, int k) {
        Objects.requireNonNull(rankedItems, "rankedItems must not be null");
        Objects.requireNonNull(isPositive, "isPositive predicate must not be null");
        if (k <= 0 || rankedItems.isEmpty() || totalPositives <= 0) {
            return 0.0;
        }

        int limit = Math.min(k, rankedItems.size());
        long positiveCount = 0;
        for (int i = 0; i < limit; i++) {
            if (isPositive.test(rankedItems.get(i))) {
                positiveCount++;
            }
        }

        double recall = (double) positiveCount / totalPositives;
        return Math.min(1.0, Math.max(0.0, recall));
    }

    /**
     * Calculates HitRate@K (1.0 if at least one positive item is in top K, else 0.0).
     *
     * @param rankedItems items pre-sorted by rank score descending, must not be null
     * @param isPositive  predicate function determining positive outcome
     * @param k           top-K cutoff threshold (must be &gt; 0)
     * @param <T>         item type
     * @return 1.0 if hit, 0.0 otherwise
     */
    public static <T> double hitRateAtK(List<T> rankedItems, java.util.function.Predicate<T> isPositive, int k) {
        Objects.requireNonNull(rankedItems, "rankedItems must not be null");
        Objects.requireNonNull(isPositive, "isPositive predicate must not be null");
        if (k <= 0 || rankedItems.isEmpty()) {
            return 0.0;
        }

        int limit = Math.min(k, rankedItems.size());
        for (int i = 0; i < limit; i++) {
            if (isPositive.test(rankedItems.get(i))) {
                return 1.0;
            }
        }

        return 0.0;
    }

    /**
     * Calculates Spearman rank correlation between scores and observed target values.
     * Fractional ranks with average tie resolution are computed for both vectors.
     *
     * @param items       items to evaluate, must not be null
     * @param scoreExtractor function extracting score (X)
     * @param valueExtractor function extracting actual target outcome (Y)
     * @param <T>         item type
     * @return Spearman rank correlation in [-1.0, 1.0], or 0.0 if variance is zero or size &lt; 2
     */
    public static <T> double spearmanCorrelation(
            List<T> items,
            ToDoubleFunction<T> scoreExtractor,
            ToDoubleFunction<T> valueExtractor
    ) {
        Objects.requireNonNull(items, "items must not be null");
        Objects.requireNonNull(scoreExtractor, "scoreExtractor must not be null");
        Objects.requireNonNull(valueExtractor, "valueExtractor must not be null");

        int n = items.size();
        if (n < 2) {
            return 0.0;
        }

        double[] x = new double[n];
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            T item = items.get(i);
            x[i] = scoreExtractor.applyAsDouble(item);
            y[i] = valueExtractor.applyAsDouble(item);
        }

        double[] rankX = computeFractionalRanks(x);
        double[] rankY = computeFractionalRanks(y);

        return computePearsonCorrelation(rankX, rankY);
    }

    /**
     * Calculates ROC-AUC (Area Under the Receiver Operating Characteristic curve)
     * for predicting binary target positive outcomes from continuous scores.
     * Uses the Mann-Whitney U statistic with fractional rank tie adjustments.
     *
     * @param items          items to evaluate, must not be null
     * @param scoreExtractor function extracting continuous score
     * @param isPositive     predicate for binary ground truth label
     * @param <T>            item type
     * @return ROC-AUC in [0.0, 1.0], or 0.5 if all labels are identical or items &lt; 2
     */
    public static <T> double rocAreaUnderCurve(
            List<T> items,
            ToDoubleFunction<T> scoreExtractor,
            java.util.function.Predicate<T> isPositive
    ) {
        Objects.requireNonNull(items, "items must not be null");
        Objects.requireNonNull(scoreExtractor, "scoreExtractor must not be null");
        Objects.requireNonNull(isPositive, "isPositive predicate must not be null");

        int n = items.size();
        if (n < 2) {
            return 0.5;
        }

        double[] scores = new double[n];
        boolean[] labels = new boolean[n];
        long positiveCount = 0;

        for (int i = 0; i < n; i++) {
            T item = items.get(i);
            scores[i] = scoreExtractor.applyAsDouble(item);
            boolean pos = isPositive.test(item);
            labels[i] = pos;
            if (pos) {
                positiveCount++;
            }
        }

        long negativeCount = n - positiveCount;
        if (positiveCount == 0 || negativeCount == 0) {
            // Indeterminate / neutral baseline when all instances belong to the same class
            return 0.5;
        }

        // Fractional ranks computed in ascending order of score
        double[] ranks = computeFractionalRanks(scores);

        double rankSumPositives = 0.0;
        for (int i = 0; i < n; i++) {
            if (labels[i]) {
                rankSumPositives += ranks[i];
            }
        }

        double u = rankSumPositives - (double) (positiveCount * (positiveCount + 1)) / 2.0;
        double auc = u / ((double) positiveCount * negativeCount);

        return Math.min(1.0, Math.max(0.0, auc));
    }

    /**
     * Computes 1-based fractional ranks (average ranks for ties) for an array of values in ascending order.
     *
     * @param values input double values
     * @return fractional rank array of same length
     */
    public static double[] computeFractionalRanks(double[] values) {
        int n = values.length;
        double[] ranks = new double[n];
        if (n == 0) {
            return ranks;
        }

        Integer[] indices = new Integer[n];
        for (int i = 0; i < n; i++) {
            indices[i] = i;
        }

        Arrays.sort(indices, Comparator.comparingDouble(i -> values[i]));

        int i = 0;
        while (i < n) {
            int j = i;
            double currentVal = values[indices[i]];
            while (j < n && Double.compare(values[indices[j]], currentVal) == 0) {
                j++;
            }

            // Average 1-based rank for range [i+1, j]
            double averageRank = (double) ((i + 1) + j) / 2.0;
            for (int k = i; k < j; k++) {
                ranks[indices[k]] = averageRank;
            }
            i = j;
        }

        return ranks;
    }

    private static double computePearsonCorrelation(double[] x, double[] y) {
        int n = x.length;
        if (n == 0) {
            return 0.0;
        }

        double sumX = 0.0;
        double sumY = 0.0;
        for (int i = 0; i < n; i++) {
            sumX += x[i];
            sumY += y[i];
        }
        double meanX = sumX / n;
        double meanY = sumY / n;

        double numerator = 0.0;
        double sumSqX = 0.0;
        double sumSqY = 0.0;

        for (int i = 0; i < n; i++) {
            double diffX = x[i] - meanX;
            double diffY = y[i] - meanY;
            numerator += diffX * diffY;
            sumSqX += diffX * diffX;
            sumSqY += diffY * diffY;
        }

        if (sumSqX <= 1e-12 || sumSqY <= 1e-12) {
            return 0.0;
        }

        double denom = Math.sqrt(sumSqX * sumSqY);
        double r = numerator / denom;
        return Math.min(1.0, Math.max(-1.0, r));
    }
}
