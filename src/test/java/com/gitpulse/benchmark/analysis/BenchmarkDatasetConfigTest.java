package com.gitpulse.benchmark.analysis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BenchmarkDatasetConfigTest {

    @Test
    @DisplayName("Standard dataset configuration contains 15 valid repositories with expected defaults")
    void standardDatasetValidation() {
        BenchmarkDatasetConfig config = BenchmarkDatasetConfig.createStandardDataset();

        assertThat(config.horizonDuration()).isEqualTo(BenchmarkDatasetConfig.DEFAULT_HORIZON);
        assertThat(config.cutoffsPerRepository()).isEqualTo(3);
        assertThat(config.primaryMetric()).isEqualTo("precisionAt10");
        assertThat(config.bootstrapResamples()).isEqualTo(10_000);
        assertThat(config.randomSeed()).isEqualTo(20260921L);

        assertThat(config.targetRepositories()).hasSize(15);
        for (BenchmarkDatasetConfig.RepositoryBenchmarkSpec spec : config.targetRepositories()) {
            assertThat(spec.owner()).isNotBlank();
            assertThat(spec.name()).isNotBlank();
            assertThat(spec.fullName()).isEqualTo(spec.owner() + "/" + spec.name());
            assertThat(spec.sizeCategory()).isIn("Small", "Medium", "Large");
            assertThat(spec.description()).isNotBlank();
        }
    }
}
