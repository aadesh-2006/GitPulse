package com.gitpulse.benchmark.analysis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BenchmarkSubgroupAnalyzerTest {

    @Test
    @DisplayName("Should classify size correctly based on historical file count thresholds")
    void shouldClassifySize() {
        assertThat(BenchmarkSubgroupAnalyzer.classifySize(10)).isEqualTo(BenchmarkSubgroupAnalyzer.SizeSubgroup.SMALL);
        assertThat(BenchmarkSubgroupAnalyzer.classifySize(49)).isEqualTo(BenchmarkSubgroupAnalyzer.SizeSubgroup.SMALL);
        assertThat(BenchmarkSubgroupAnalyzer.classifySize(50)).isEqualTo(BenchmarkSubgroupAnalyzer.SizeSubgroup.MEDIUM);
        assertThat(BenchmarkSubgroupAnalyzer.classifySize(500)).isEqualTo(BenchmarkSubgroupAnalyzer.SizeSubgroup.MEDIUM);
        assertThat(BenchmarkSubgroupAnalyzer.classifySize(501)).isEqualTo(BenchmarkSubgroupAnalyzer.SizeSubgroup.LARGE);
        assertThat(BenchmarkSubgroupAnalyzer.classifySize(5000)).isEqualTo(BenchmarkSubgroupAnalyzer.SizeSubgroup.LARGE);
    }

    @Test
    @DisplayName("Should classify activity correctly based on future active ratio")
    void shouldClassifyActivity() {
        assertThat(BenchmarkSubgroupAnalyzer.classifyActivity(0, 100)).isEqualTo(BenchmarkSubgroupAnalyzer.ActivitySubgroup.LOWER_ACTIVITY);
        assertThat(BenchmarkSubgroupAnalyzer.classifyActivity(15, 100)).isEqualTo(BenchmarkSubgroupAnalyzer.ActivitySubgroup.LOWER_ACTIVITY);
        assertThat(BenchmarkSubgroupAnalyzer.classifyActivity(20, 100)).isEqualTo(BenchmarkSubgroupAnalyzer.ActivitySubgroup.HIGHER_ACTIVITY);
        assertThat(BenchmarkSubgroupAnalyzer.classifyActivity(50, 100)).isEqualTo(BenchmarkSubgroupAnalyzer.ActivitySubgroup.HIGHER_ACTIVITY);
    }
}
