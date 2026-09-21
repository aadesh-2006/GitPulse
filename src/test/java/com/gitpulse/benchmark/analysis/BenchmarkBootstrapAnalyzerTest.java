package com.gitpulse.benchmark.analysis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class BenchmarkBootstrapAnalyzerTest {

    private record TestRun(double baseline, double composite) {}

    @Test
    @DisplayName("Bootstrap confidence interval calculation is 100% deterministic with fixed seed")
    void bootstrapDeterminism() {
        List<TestRun> runs = List.of(
                new TestRun(0.40, 0.50),
                new TestRun(0.30, 0.35),
                new TestRun(0.60, 0.60),
                new TestRun(0.50, 0.70),
                new TestRun(0.20, 0.30),
                new TestRun(0.70, 0.80)
        );

        long seed = 20260921L;
        int resamples = 2000;

        BenchmarkBootstrapAnalyzer.PairedBootstrapResult r1 = BenchmarkBootstrapAnalyzer.calculatePairedBootstrap(
                runs, TestRun::baseline, TestRun::composite, resamples, seed
        );

        BenchmarkBootstrapAnalyzer.PairedBootstrapResult r2 = BenchmarkBootstrapAnalyzer.calculatePairedBootstrap(
                runs, TestRun::baseline, TestRun::composite, resamples, seed
        );

        assertThat(r1.deltaCi().mean()).isEqualTo(r2.deltaCi().mean());
        assertThat(r1.deltaCi().lowerBound95()).isEqualTo(r2.deltaCi().lowerBound95());
        assertThat(r1.deltaCi().upperBound95()).isEqualTo(r2.deltaCi().upperBound95());

        // Confidence interval bounds must enclose the observed mean
        assertThat(r1.deltaCi().lowerBound95()).isLessThanOrEqualTo(r1.deltaCi().mean());
        assertThat(r1.deltaCi().upperBound95()).isGreaterThanOrEqualTo(r1.deltaCi().mean());
    }

    @Test
    @DisplayName("Bootstrap handles empty list safely")
    void bootstrapEmptyList() {
        BenchmarkBootstrapAnalyzer.PairedBootstrapResult res = BenchmarkBootstrapAnalyzer.calculatePairedBootstrap(
                List.of(), TestRun::baseline, TestRun::composite, 1000, 42L
        );

        assertThat(res.deltaCi().mean()).isEqualTo(0.0);
        assertThat(res.deltaCi().lowerBound95()).isEqualTo(0.0);
        assertThat(res.deltaCi().upperBound95()).isEqualTo(0.0);
    }
}
