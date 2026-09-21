package com.gitpulse.benchmark.analysis;

import com.gitpulse.benchmark.dto.BenchmarkRankingMetrics;
import com.gitpulse.benchmark.dto.BenchmarkResult;
import com.gitpulse.benchmark.dto.BenchmarkSignalComparison;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BenchmarkCsvExporterTest {

    @Test
    @DisplayName("CSV export contains no NaN or Infinity even with edge case inputs")
    void csvExportNoNaNOrInfinity() {
        BenchmarkRankingMetrics baseline = new BenchmarkRankingMetrics(
                "baseline", 0.5, 0.4, 0.3, 0.5, 0.4, 0.3, 1.0, 1.0, 1.0, 0.25, 0.20, 0.70
        );
        BenchmarkRankingMetrics composite = new BenchmarkRankingMetrics(
                "composite", 0.6, 0.5, 0.4, 0.6, 0.5, 0.4, 1.0, 1.0, 1.0, 0.35, 0.30, 0.80
        );

        BenchmarkResult result = new BenchmarkResult(
                1L, "octocat/Hello-World", Instant.parse("2024-01-01T00:00:00Z"), Duration.ofDays(90),
                50, 10, 8, 2,
                baseline, composite,
                Map.of("baseline", baseline, "composite", composite),
                BenchmarkSignalComparison.of(baseline, composite),
                120L
        );

        String runsCsv = BenchmarkCsvExporter.exportBenchmarkRunsCsv(List.of(result));
        assertThat(runsCsv).doesNotContain("NaN");
        assertThat(runsCsv).doesNotContain("Infinity");
        assertThat(runsCsv).contains("octocat/Hello-World");
        assertThat(runsCsv).contains("0.400000");
        assertThat(runsCsv).contains("0.500000");

        String ablationCsv = BenchmarkCsvExporter.exportAblationSummaryCsv(List.of(result));
        assertThat(ablationCsv).doesNotContain("NaN");
        assertThat(ablationCsv).doesNotContain("Infinity");
        assertThat(ablationCsv).contains("baseline");
        assertThat(ablationCsv).contains("composite");
    }
}
