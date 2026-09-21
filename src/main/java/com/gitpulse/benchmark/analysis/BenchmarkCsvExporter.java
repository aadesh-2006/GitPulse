package com.gitpulse.benchmark.analysis;

import com.gitpulse.benchmark.dto.BenchmarkRankingMetrics;
import com.gitpulse.benchmark.dto.BenchmarkResult;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Safe, reproducible CSV serializer for benchmark results without NaN or Infinity values.
 */
public final class BenchmarkCsvExporter {

    private BenchmarkCsvExporter() {
        // Utility class
    }

    /**
     * Exports individual benchmark runs to CSV string.
     */
    public static String exportBenchmarkRunsCsv(List<BenchmarkResult> results) {
        Objects.requireNonNull(results, "results must not be null");

        StringBuilder sb = new StringBuilder();
        sb.append("repository,repositoryId,cutoff,horizonDays,historicalFiles,futureActiveFiles,futureChangedFiles,newFiles,")
                .append("baselinePrecisionAt5,baselinePrecisionAt10,baselinePrecisionAt20,")
                .append("compositePrecisionAt5,compositePrecisionAt10,compositePrecisionAt20,")
                .append("deltaPrecisionAt10,")
                .append("baselineRecallAt10,compositeRecallAt10,deltaRecallAt10,")
                .append("baselineSpearmanRevs,compositeSpearmanRevs,deltaSpearmanRevs,")
                .append("baselineRocAuc,compositeRocAuc,deltaRocAuc\n");

        for (BenchmarkResult r : results) {
            sb.append(escapeCsv(r.repositoryName())).append(",")
                    .append(r.repositoryId()).append(",")
                    .append(r.cutoffTime()).append(",")
                    .append(r.horizonDuration().toDays()).append(",")
                    .append(r.totalHistoricalFiles()).append(",")
                    .append(r.totalActiveFutureFiles()).append(",")
                    .append(r.futureChangedHistoricalFiles()).append(",")
                    .append(r.newFilesIntroducedInFuture()).append(",")
                    .append(formatDouble(r.baselineMetrics().precisionAt5())).append(",")
                    .append(formatDouble(r.baselineMetrics().precisionAt10())).append(",")
                    .append(formatDouble(r.baselineMetrics().precisionAt20())).append(",")
                    .append(formatDouble(r.compositeMetrics().precisionAt5())).append(",")
                    .append(formatDouble(r.compositeMetrics().precisionAt10())).append(",")
                    .append(formatDouble(r.compositeMetrics().precisionAt20())).append(",")
                    .append(formatDouble(r.comparisonSummary().precisionDeltaAt10())).append(",")
                    .append(formatDouble(r.baselineMetrics().recallAt10())).append(",")
                    .append(formatDouble(r.compositeMetrics().recallAt10())).append(",")
                    .append(formatDouble(r.comparisonSummary().recallDeltaAt10())).append(",")
                    .append(formatDouble(r.baselineMetrics().spearmanCorrelationRevisions())).append(",")
                    .append(formatDouble(r.compositeMetrics().spearmanCorrelationRevisions())).append(",")
                    .append(formatDouble(r.comparisonSummary().spearmanRevisionsDelta())).append(",")
                    .append(formatDouble(r.baselineMetrics().rocAreaUnderCurve())).append(",")
                    .append(formatDouble(r.compositeMetrics().rocAreaUnderCurve())).append(",")
                    .append(formatDouble(r.comparisonSummary().rocAucDelta()))
                    .append("\n");
        }

        return sb.toString();
    }

    /**
     * Exports aggregate metrics summary to CSV string.
     */
    public static String exportMetricSummaryCsv(
            BenchmarkAggregateStatistics.PairedComparisonSummary precision10Summary,
            BenchmarkAggregateStatistics.PairedComparisonSummary recall10Summary,
            BenchmarkAggregateStatistics.PairedComparisonSummary spearmanSummary,
            BenchmarkAggregateStatistics.PairedComparisonSummary rocAucSummary
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("metric,baselineMean,baselineMedian,baselineStd,compositeMean,compositeMedian,compositeStd,meanDelta,medianDelta,compositeHigher,baselineHigher,ties\n");

        appendMetricRow(sb, "precisionAt10", precision10Summary);
        appendMetricRow(sb, "recallAt10", recall10Summary);
        appendMetricRow(sb, "spearmanRevisions", spearmanSummary);
        appendMetricRow(sb, "rocAuc", rocAucSummary);

        return sb.toString();
    }

    /**
     * Exports ablation summary across all signals for Precision@10.
     */
    public static String exportAblationSummaryCsv(List<BenchmarkResult> results) {
        Objects.requireNonNull(results, "results must not be null");

        StringBuilder sb = new StringBuilder();
        sb.append("signal,meanPrecisionAt10,medianPrecisionAt10,stdDevPrecisionAt10,minPrecisionAt10,maxPrecisionAt10,runCount\n");

        List<String> signals = List.of("baseline", "composite", "revisionFrequency", "churn", "recency", "ownershipConcentration");
        for (String sig : signals) {
            List<Double> values = results.stream()
                    .map(r -> {
                        BenchmarkRankingMetrics m = r.ablationMetrics().get(sig);
                        return m != null ? m.precisionAt10() : null;
                    })
                    .filter(Objects::nonNull)
                    .toList();

            BenchmarkAggregateStatistics.DistributionSummary dist = BenchmarkAggregateStatistics.calculateDistribution(values);
            sb.append(sig).append(",")
                    .append(formatDouble(dist.mean())).append(",")
                    .append(formatDouble(dist.median())).append(",")
                    .append(formatDouble(dist.standardDeviation())).append(",")
                    .append(formatDouble(dist.min())).append(",")
                    .append(formatDouble(dist.max())).append(",")
                    .append(dist.count())
                    .append("\n");
        }

        return sb.toString();
    }

    /**
     * Exports bootstrap confidence intervals to CSV string.
     */
    public static String exportBootstrapSummaryCsv(
            BenchmarkBootstrapAnalyzer.PairedBootstrapResult precision10Bootstrap
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("target,observedMean,lowerBound95,upperBound95,resampleCount,seed\n");

        appendBootstrapRow(sb, "baselinePrecisionAt10", precision10Bootstrap.baselineCi());
        appendBootstrapRow(sb, "compositePrecisionAt10", precision10Bootstrap.compositeCi());
        appendBootstrapRow(sb, "deltaPrecisionAt10", precision10Bootstrap.deltaCi());

        return sb.toString();
    }

    private static void appendMetricRow(StringBuilder sb, String metricName, BenchmarkAggregateStatistics.PairedComparisonSummary s) {
        if (s == null) return;
        sb.append(metricName).append(",")
                .append(formatDouble(s.baselineDistribution().mean())).append(",")
                .append(formatDouble(s.baselineDistribution().median())).append(",")
                .append(formatDouble(s.baselineDistribution().standardDeviation())).append(",")
                .append(formatDouble(s.compositeDistribution().mean())).append(",")
                .append(formatDouble(s.compositeDistribution().median())).append(",")
                .append(formatDouble(s.compositeDistribution().standardDeviation())).append(",")
                .append(formatDouble(s.deltaDistribution().mean())).append(",")
                .append(formatDouble(s.deltaDistribution().median())).append(",")
                .append(s.compositeHigherCount()).append(",")
                .append(s.baselineHigherCount()).append(",")
                .append(s.tieCount())
                .append("\n");
    }

    private static void appendBootstrapRow(StringBuilder sb, String targetName, BenchmarkBootstrapAnalyzer.BootstrapConfidenceInterval ci) {
        if (ci == null) return;
        sb.append(targetName).append(",")
                .append(formatDouble(ci.mean())).append(",")
                .append(formatDouble(ci.lowerBound95())).append(",")
                .append(formatDouble(ci.upperBound95())).append(",")
                .append(ci.resampleCount()).append(",")
                .append(ci.randomSeed())
                .append("\n");
    }

    private static String formatDouble(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "NA";
        }
        return String.format(Locale.US, "%.6f", value);
    }

    private static String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}
