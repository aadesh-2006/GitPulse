package com.gitpulse.benchmark.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.ToDoubleFunction;

/**
 * Deterministic bootstrap confidence interval calculation engine for cross-repository benchmark evaluations.
 */
public final class BenchmarkBootstrapAnalyzer {

    private BenchmarkBootstrapAnalyzer() {
        // Utility class
    }

    public record BootstrapConfidenceInterval(
            double mean,
            double lowerBound95,
            double upperBound95,
            int resampleCount,
            long randomSeed
    ) {
        public static BootstrapConfidenceInterval empty() {
            return new BootstrapConfidenceInterval(0.0, 0.0, 0.0, 0, 0L);
        }
    }

    public record PairedBootstrapResult(
            BootstrapConfidenceInterval baselineCi,
            BootstrapConfidenceInterval compositeCi,
            BootstrapConfidenceInterval deltaCi
    ) {
    }

    /**
     * Computes deterministic 95% bootstrap confidence intervals for baseline, composite, and paired deltas.
     *
     * @param runs               list of benchmark runs, must not be null
     * @param baselineExtractor  function extracting baseline metric
     * @param compositeExtractor function extracting composite metric
     * @param resampleCount      number of bootstrap resamples (e.g. 10,000)
     * @param randomSeed         deterministic PRNG seed
     * @param <T>                benchmark run type
     * @return {@link PairedBootstrapResult}
     */
    public static <T> PairedBootstrapResult calculatePairedBootstrap(
            List<T> runs,
            ToDoubleFunction<T> baselineExtractor,
            ToDoubleFunction<T> compositeExtractor,
            int resampleCount,
            long randomSeed
    ) {
        Objects.requireNonNull(runs, "runs must not be null");
        Objects.requireNonNull(baselineExtractor, "baselineExtractor must not be null");
        Objects.requireNonNull(compositeExtractor, "compositeExtractor must not be null");

        if (runs.isEmpty() || resampleCount <= 0) {
            return new PairedBootstrapResult(
                    BootstrapConfidenceInterval.empty(),
                    BootstrapConfidenceInterval.empty(),
                    BootstrapConfidenceInterval.empty()
            );
        }

        int n = runs.size();
        double[] baselineArr = new double[n];
        double[] compositeArr = new double[n];
        double[] deltaArr = new double[n];

        for (int i = 0; i < n; i++) {
            T run = runs.get(i);
            double b = baselineExtractor.applyAsDouble(run);
            double c = compositeExtractor.applyAsDouble(run);
            baselineArr[i] = b;
            compositeArr[i] = c;
            deltaArr[i] = c - b;
        }

        Random random = new Random(randomSeed);
        List<Double> baselineResampleMeans = new ArrayList<>(resampleCount);
        List<Double> compositeResampleMeans = new ArrayList<>(resampleCount);
        List<Double> deltaResampleMeans = new ArrayList<>(resampleCount);

        for (int r = 0; r < resampleCount; r++) {
            double sumB = 0.0;
            double sumC = 0.0;
            double sumD = 0.0;

            for (int i = 0; i < n; i++) {
                int sampledIdx = random.nextInt(n);
                sumB += baselineArr[sampledIdx];
                sumC += compositeArr[sampledIdx];
                sumD += deltaArr[sampledIdx];
            }

            baselineResampleMeans.add(sumB / n);
            compositeResampleMeans.add(sumC / n);
            deltaResampleMeans.add(sumD / n);
        }

        BootstrapConfidenceInterval baselineCi = extractInterval(baselineResampleMeans, mean(baselineArr), resampleCount, randomSeed);
        BootstrapConfidenceInterval compositeCi = extractInterval(compositeResampleMeans, mean(compositeArr), resampleCount, randomSeed);
        BootstrapConfidenceInterval deltaCi = extractInterval(deltaResampleMeans, mean(deltaArr), resampleCount, randomSeed);

        return new PairedBootstrapResult(baselineCi, compositeCi, deltaCi);
    }

    private static BootstrapConfidenceInterval extractInterval(
            List<Double> resampleMeans,
            double observedMean,
            int resampleCount,
            long seed
    ) {
        Collections.sort(resampleMeans);
        int lowerIdx = (int) Math.floor(resampleCount * 0.025);
        int upperIdx = (int) Math.ceil(resampleCount * 0.975) - 1;

        lowerIdx = Math.max(0, Math.min(lowerIdx, resampleCount - 1));
        upperIdx = Math.max(0, Math.min(upperIdx, resampleCount - 1));

        double lower = resampleMeans.get(lowerIdx);
        double upper = resampleMeans.get(upperIdx);

        return new BootstrapConfidenceInterval(observedMean, lower, upper, resampleCount, seed);
    }

    private static double mean(double[] arr) {
        if (arr.length == 0) return 0.0;
        double sum = 0.0;
        for (double v : arr) sum += v;
        return sum / arr.length;
    }
}
