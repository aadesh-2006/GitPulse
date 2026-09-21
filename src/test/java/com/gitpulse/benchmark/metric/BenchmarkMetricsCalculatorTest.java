package com.gitpulse.benchmark.metric;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class BenchmarkMetricsCalculatorTest {

    private record TestItem(String name, double score, long futureRevisions, long futureChurn, boolean futureChanged) {}

    @Nested
    @DisplayName("Precision@K Tests")
    class PrecisionAtKTests {

        @Test
        @DisplayName("Should return 0.0 for empty list or invalid k")
        void shouldHandleEmptyOrInvalidK() {
            assertThat(BenchmarkMetricsCalculator.precisionAtK(Collections.emptyList(), TestItem::futureChanged, 5)).isEqualTo(0.0);
            assertThat(BenchmarkMetricsCalculator.precisionAtK(List.of(new TestItem("a", 1.0, 1, 10, true)), TestItem::futureChanged, 0)).isEqualTo(0.0);
            assertThat(BenchmarkMetricsCalculator.precisionAtK(List.of(new TestItem("a", 1.0, 1, 10, true)), TestItem::futureChanged, -1)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should calculate exact Precision@5 on ranked list")
        void shouldCalculateExactPrecisionAt5() {
            List<TestItem> items = List.of(
                    new TestItem("f1", 0.9, 5, 50, true),
                    new TestItem("f2", 0.8, 3, 30, true),
                    new TestItem("f3", 0.7, 0, 0, false),
                    new TestItem("f4", 0.6, 2, 20, true),
                    new TestItem("f5", 0.5, 0, 0, false),
                    new TestItem("f6", 0.4, 1, 10, true)
            );

            // Top 5: [f1(true), f2(true), f3(false), f4(true), f5(false)] -> 3 true out of 5 -> 0.60
            double p5 = BenchmarkMetricsCalculator.precisionAtK(items, TestItem::futureChanged, 5);
            assertThat(p5).isCloseTo(0.60, within(1e-9));

            // Top 3: [f1(true), f2(true), f3(false)] -> 2/3 = 0.66666...
            double p3 = BenchmarkMetricsCalculator.precisionAtK(items, TestItem::futureChanged, 3);
            assertThat(p3).isCloseTo(2.0 / 3.0, within(1e-9));
        }

        @Test
        @DisplayName("Should adjust denominator when total items < k")
        void shouldAdjustDenominatorWhenFewerItemsThanK() {
            List<TestItem> items = List.of(
                    new TestItem("f1", 0.9, 5, 50, true),
                    new TestItem("f2", 0.8, 0, 0, false)
            );

            // 2 items total, k = 5, 1 positive -> 1 / 2 = 0.50
            double p5 = BenchmarkMetricsCalculator.precisionAtK(items, TestItem::futureChanged, 5);
            assertThat(p5).isCloseTo(0.50, within(1e-9));
        }
    }

    @Nested
    @DisplayName("Recall@K Tests")
    class RecallAtKTests {

        @Test
        @DisplayName("Should return 0.0 for empty list or zero total positives")
        void shouldHandleEmptyOrZeroPositives() {
            assertThat(BenchmarkMetricsCalculator.recallAtK(Collections.emptyList(), TestItem::futureChanged, 5, 5)).isEqualTo(0.0);
            assertThat(BenchmarkMetricsCalculator.recallAtK(List.of(new TestItem("a", 1.0, 1, 10, true)), TestItem::futureChanged, 0, 5)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should calculate exact Recall@K relative to total positives")
        void shouldCalculateExactRecallAtK() {
            List<TestItem> items = List.of(
                    new TestItem("f1", 0.9, 5, 50, true),
                    new TestItem("f2", 0.8, 0, 0, false),
                    new TestItem("f3", 0.7, 2, 20, true),
                    new TestItem("f4", 0.6, 0, 0, false),
                    new TestItem("f5", 0.5, 1, 10, true),
                    new TestItem("f6", 0.4, 4, 40, true)
            );

            // Total positives across repository = 4 (f1, f3, f5, f6)
            // Top 3 items: [f1(true), f2(false), f3(true)] -> 2 positives captured -> 2 / 4 = 0.50
            double r3 = BenchmarkMetricsCalculator.recallAtK(items, TestItem::futureChanged, 4, 3);
            assertThat(r3).isCloseTo(0.50, within(1e-9));

            // Top 5 items: [f1(true), f2(false), f3(true), f4(false), f5(true)] -> 3 positives captured -> 3 / 4 = 0.75
            double r5 = BenchmarkMetricsCalculator.recallAtK(items, TestItem::futureChanged, 4, 5);
            assertThat(r5).isCloseTo(0.75, within(1e-9));
        }
    }

    @Nested
    @DisplayName("HitRate@K Tests")
    class HitRateAtKTests {

        @Test
        @DisplayName("Should return 1.0 if any item in top K is positive, else 0.0")
        void shouldCalculateHitRate() {
            List<TestItem> items = List.of(
                    new TestItem("f1", 0.9, 0, 0, false),
                    new TestItem("f2", 0.8, 0, 0, false),
                    new TestItem("f3", 0.7, 2, 20, true),
                    new TestItem("f4", 0.6, 0, 0, false)
            );

            assertThat(BenchmarkMetricsCalculator.hitRateAtK(items, TestItem::futureChanged, 2)).isEqualTo(0.0);
            assertThat(BenchmarkMetricsCalculator.hitRateAtK(items, TestItem::futureChanged, 3)).isEqualTo(1.0);
            assertThat(BenchmarkMetricsCalculator.hitRateAtK(items, TestItem::futureChanged, 5)).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("Fractional Ranks Tests")
    class FractionalRanksTests {

        @Test
        @DisplayName("Should compute strict fractional ranks for strictly increasing values")
        void shouldComputeStrictRanks() {
            double[] values = {10.0, 20.0, 30.0, 40.0};
            double[] ranks = BenchmarkMetricsCalculator.computeFractionalRanks(values);
            assertThat(ranks).containsExactly(1.0, 2.0, 3.0, 4.0);
        }

        @Test
        @DisplayName("Should compute average fractional ranks for ties")
        void shouldComputeAverageRanksForTies() {
            // sorted: 10.0 (idx 0 -> rank 1), 20.0 (idx 1,2 -> rank (2+3)/2=2.5), 30.0 (idx 3 -> rank 4)
            double[] values = {10.0, 20.0, 20.0, 30.0};
            double[] ranks = BenchmarkMetricsCalculator.computeFractionalRanks(values);
            assertThat(ranks).containsExactly(1.0, 2.5, 2.5, 4.0);
        }

        @Test
        @DisplayName("Should assign same rank to all identical elements")
        void shouldAssignSameRankToIdenticalElements() {
            double[] values = {5.0, 5.0, 5.0, 5.0};
            double[] ranks = BenchmarkMetricsCalculator.computeFractionalRanks(values);
            // (1+2+3+4)/4 = 2.5
            assertThat(ranks).containsExactly(2.5, 2.5, 2.5, 2.5);
        }
    }

    @Nested
    @DisplayName("Spearman Rank Correlation Tests")
    class SpearmanCorrelationTests {

        @Test
        @DisplayName("Should return 1.0 for perfectly monotonic relationship")
        void shouldReturnOneForPerfectMonotonic() {
            List<TestItem> items = List.of(
                    new TestItem("f1", 0.1, 1, 10, true),
                    new TestItem("f2", 0.3, 3, 30, true),
                    new TestItem("f3", 0.6, 7, 70, true),
                    new TestItem("f4", 0.9, 12, 120, true)
            );

            double rho = BenchmarkMetricsCalculator.spearmanCorrelation(
                    items,
                    TestItem::score,
                    item -> (double) item.futureRevisions()
            );

            assertThat(rho).isCloseTo(1.0, within(1e-9));
        }

        @Test
        @DisplayName("Should return -1.0 for perfectly inverse monotonic relationship")
        void shouldReturnMinusOneForInverseMonotonic() {
            List<TestItem> items = List.of(
                    new TestItem("f1", 0.1, 12, 120, true),
                    new TestItem("f2", 0.3, 7, 70, true),
                    new TestItem("f3", 0.6, 3, 30, true),
                    new TestItem("f4", 0.9, 1, 10, true)
            );

            double rho = BenchmarkMetricsCalculator.spearmanCorrelation(
                    items,
                    TestItem::score,
                    item -> (double) item.futureRevisions()
            );

            assertThat(rho).isCloseTo(-1.0, within(1e-9));
        }

        @Test
        @DisplayName("Should return 0.0 when variance of either vector is zero")
        void shouldReturnZeroForZeroVariance() {
            List<TestItem> items = List.of(
                    new TestItem("f1", 0.5, 0, 0, false),
                    new TestItem("f2", 0.5, 0, 0, false),
                    new TestItem("f3", 0.5, 0, 0, false)
            );

            double rho = BenchmarkMetricsCalculator.spearmanCorrelation(
                    items,
                    TestItem::score,
                    item -> (double) item.futureRevisions()
            );

            assertThat(rho).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("ROC-AUC Tests")
    class RocAucTests {

        @Test
        @DisplayName("Should return 1.0 for perfectly separated classes")
        void shouldReturnOneForPerfectSeparation() {
            // Positives have higher scores than negatives
            List<TestItem> items = List.of(
                    new TestItem("f1", 0.9, 5, 50, true),
                    new TestItem("f2", 0.8, 3, 30, true),
                    new TestItem("f3", 0.3, 0, 0, false),
                    new TestItem("f4", 0.2, 0, 0, false)
            );

            double auc = BenchmarkMetricsCalculator.rocAreaUnderCurve(
                    items,
                    TestItem::score,
                    TestItem::futureChanged
            );

            assertThat(auc).isCloseTo(1.0, within(1e-9));
        }

        @Test
        @DisplayName("Should return 0.0 for completely inverted classes")
        void shouldReturnZeroForInvertedClasses() {
            // Negatives have higher scores than positives
            List<TestItem> items = List.of(
                    new TestItem("f1", 0.1, 5, 50, true),
                    new TestItem("f2", 0.2, 3, 30, true),
                    new TestItem("f3", 0.8, 0, 0, false),
                    new TestItem("f4", 0.9, 0, 0, false)
            );

            double auc = BenchmarkMetricsCalculator.rocAreaUnderCurve(
                    items,
                    TestItem::score,
                    TestItem::futureChanged
            );

            assertThat(auc).isCloseTo(0.0, within(1e-9));
        }

        @Test
        @DisplayName("Should return 0.5 for tied scores or indeterminate single-class datasets")
        void shouldReturnHalfForTiedOrSingleClass() {
            List<TestItem> tied = List.of(
                    new TestItem("f1", 0.5, 5, 50, true),
                    new TestItem("f2", 0.5, 0, 0, false)
            );
            assertThat(BenchmarkMetricsCalculator.rocAreaUnderCurve(tied, TestItem::score, TestItem::futureChanged)).isCloseTo(0.5, within(1e-9));

            List<TestItem> allPositives = List.of(
                    new TestItem("f1", 0.9, 5, 50, true),
                    new TestItem("f2", 0.8, 3, 30, true)
            );
            assertThat(BenchmarkMetricsCalculator.rocAreaUnderCurve(allPositives, TestItem::score, TestItem::futureChanged)).isEqualTo(0.5);
        }

        @Test
        @DisplayName("Should match manual Mann-Whitney U calculation on mixed ranks")
        void shouldMatchManualCalculation() {
            // Items:
            // Positives: score 0.8, score 0.4
            // Negatives: score 0.6, score 0.2
            // Sorted ascending scores:
            // 0.2 (neg, rank 1)
            // 0.4 (pos, rank 2)
            // 0.6 (neg, rank 3)
            // 0.8 (pos, rank 4)
            // n1 = 2, n0 = 2
            // R1 = 2 + 4 = 6
            // U = 6 - (2 * 3) / 2 = 6 - 3 = 3
            // AUC = 3 / (2 * 2) = 0.75
            List<TestItem> items = List.of(
                    new TestItem("f1", 0.8, 2, 20, true),
                    new TestItem("f2", 0.6, 0, 0, false),
                    new TestItem("f3", 0.4, 1, 10, true),
                    new TestItem("f4", 0.2, 0, 0, false)
            );

            double auc = BenchmarkMetricsCalculator.rocAreaUnderCurve(
                    items,
                    TestItem::score,
                    TestItem::futureChanged
            );

            assertThat(auc).isCloseTo(0.75, within(1e-9));
        }
    }
}
