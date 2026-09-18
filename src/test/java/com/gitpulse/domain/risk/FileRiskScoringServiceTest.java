package com.gitpulse.domain.risk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class FileRiskScoringServiceTest {

    private FileRiskScoringService scoringService;
    private final Instant referenceTime = Instant.parse("2026-06-01T12:00:00Z");

    @BeforeEach
    void setUp() {
        scoringService = new FileRiskScoringService();
    }

    @Nested
    @DisplayName("Revision Frequency Normalization Tests")
    class RevisionFrequencyTests {

        @Test
        @DisplayName("Zero revisions yields 0.0")
        void zeroRevisions_YieldsZero() {
            double score = scoringService.calculateRevisionFrequencyScore(0, 100);
            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Max revisions zero or negative yields 0.0")
        void zeroOrNegativeMax_YieldsZero() {
            assertThat(scoringService.calculateRevisionFrequencyScore(10, 0)).isEqualTo(0.0);
            assertThat(scoringService.calculateRevisionFrequencyScore(10, -5)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Negative revisions yields 0.0")
        void negativeRevisions_YieldsZero() {
            assertThat(scoringService.calculateRevisionFrequencyScore(-10, 100)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Revisions equal to max yields exactly 1.0")
        void revisionsEqualToMax_YieldsOne() {
            double score = scoringService.calculateRevisionFrequencyScore(500, 500);
            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Revisions exceeding max is clamped to 1.0")
        void revisionsExceedingMax_ClampedToOne() {
            double score = scoringService.calculateRevisionFrequencyScore(600, 500);
            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Revisions between 0 and max yields log-normalized score in (0, 1)")
        void revisionsBetweenZeroAndMax_YieldsNormalizedScore() {
            // log1p(10) / log1p(1000) = ln(11) / ln(1001) ≈ 2.397895 / 6.908755 ≈ 0.34708
            double score = scoringService.calculateRevisionFrequencyScore(10, 1000);
            assertThat(score).isGreaterThan(0.0).isLessThan(1.0);
            assertThat(score).isCloseTo(Math.log1p(10) / Math.log1p(1000), within(1e-9));
            // Log normalization prevents 10/1000 (0.01) from being suppressed to near zero
            assertThat(score).isGreaterThan(0.01);
        }
    }

    @Nested
    @DisplayName("Churn Normalization Tests")
    class ChurnTests {

        @Test
        @DisplayName("Zero churn yields 0.0")
        void zeroChurn_YieldsZero() {
            double score = scoringService.calculateChurnScore(0, 5000);
            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Max churn zero or negative yields 0.0")
        void zeroOrNegativeMaxChurn_YieldsZero() {
            assertThat(scoringService.calculateChurnScore(500, 0)).isEqualTo(0.0);
            assertThat(scoringService.calculateChurnScore(500, -100)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Negative churn yields 0.0")
        void negativeChurn_YieldsZero() {
            assertThat(scoringService.calculateChurnScore(-50, 5000)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Churn equal to max yields exactly 1.0")
        void churnEqualToMax_YieldsOne() {
            double score = scoringService.calculateChurnScore(5000, 5000);
            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Churn exceeding max is clamped to 1.0")
        void churnExceedingMax_ClampedToOne() {
            double score = scoringService.calculateChurnScore(7500, 5000);
            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Churn between 0 and max yields log-normalized score in (0, 1)")
        void churnBetweenZeroAndMax_YieldsNormalizedScore() {
            double score = scoringService.calculateChurnScore(500, 50000);
            assertThat(score).isGreaterThan(0.0).isLessThan(1.0);
            assertThat(score).isCloseTo(Math.log1p(500) / Math.log1p(50000), within(1e-9));
        }
    }

    @Nested
    @DisplayName("Recency Exponential Decay Tests")
    class RecencyTests {

        @Test
        @DisplayName("Null lastModifiedAt yields 0.0")
        void nullLastModifiedAt_YieldsZero() {
            double score = scoringService.calculateRecencyScore(null, referenceTime);
            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Null referenceTime throws NullPointerException")
        void nullReferenceTime_ThrowsException() {
            assertThatThrownBy(() -> scoringService.calculateRecencyScore(Instant.now(), null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("referenceTime must not be null");
        }

        @Test
        @DisplayName("Modification at exact referenceTime yields 1.0")
        void sameTimestamp_YieldsOne() {
            double score = scoringService.calculateRecencyScore(referenceTime, referenceTime);
            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Future modification timestamp is clamped to 1.0")
        void futureTimestamp_YieldsOne() {
            Instant future = referenceTime.plus(Duration.ofDays(10));
            double score = scoringService.calculateRecencyScore(future, referenceTime);
            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Exactly 90 days old yields true half-life score of 0.5")
        void ninetyDaysOld_YieldsHalf() {
            Instant ninetyDaysAgo = referenceTime.minus(Duration.ofDays(90));
            double score = scoringService.calculateRecencyScore(ninetyDaysAgo, referenceTime);
            assertThat(score).isCloseTo(0.5, within(1e-9));
        }

        @Test
        @DisplayName("Exactly 180 days old yields true quarter-life score of 0.25")
        void oneHundredEightyDaysOld_YieldsQuarter() {
            Instant oneHundredEightyDaysAgo = referenceTime.minus(Duration.ofDays(180));
            double score = scoringService.calculateRecencyScore(oneHundredEightyDaysAgo, referenceTime);
            assertThat(score).isCloseTo(0.25, within(1e-9));
        }

        @Test
        @DisplayName("Exactly 270 days old yields 0.125")
        void twoHundredSeventyDaysOld_YieldsOneEighth() {
            Instant twoHundredSeventyDaysAgo = referenceTime.minus(Duration.ofDays(270));
            double score = scoringService.calculateRecencyScore(twoHundredSeventyDaysAgo, referenceTime);
            assertThat(score).isCloseTo(0.125, within(1e-9));
        }

        @Test
        @DisplayName("Very old file smoothly approaches 0.0")
        void veryOldFile_ApproachesZero() {
            Instant tenYearsAgo = referenceTime.minus(Duration.ofDays(3650));
            double score = scoringService.calculateRecencyScore(tenYearsAgo, referenceTime);
            assertThat(score).isGreaterThan(0.0).isLessThan(0.0001);
        }

        @Test
        @DisplayName("Extremely old timestamp does not overflow Duration and safely yields finite score near 0.0")
        void extremelyOldTimestamp_DoesNotOverflow_YieldsZero() {
            // An Instant 1000 years prior to referenceTime (would overflow Duration.toNanos() at ~292 years)
            Instant oneThousandYearsAgo = referenceTime.minus(Duration.ofDays(365_250));
            double score = scoringService.calculateRecencyScore(oneThousandYearsAgo, referenceTime);

            assertThat(score).isNotNaN();
            assertThat(Double.isFinite(score)).isTrue();
            assertThat(score).isBetween(0.0, 1.0);
            assertThat(score).isLessThan(1e-100);
        }
    }

    @Nested
    @DisplayName("Ownership Concentration Tests")
    class OwnershipConcentrationTests {

        @ParameterizedTest
        @CsvSource({
                "0.0, 0.0",
                "0.25, 0.25",
                "0.50, 0.50",
                "0.75, 0.75",
                "1.0, 1.0"
        })
        @DisplayName("Valid ownership concentration values pass through directly")
        void validValues_PassThrough(double input, double expected) {
            double score = scoringService.calculateOwnershipConcentrationScore(input);
            assertThat(score).isEqualTo(expected);
        }

        @Test
        @DisplayName("Negative ownership concentration is clamped to 0.0")
        void negativeValue_ClampedToZero() {
            assertThat(scoringService.calculateOwnershipConcentrationScore(-0.25)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Ownership concentration exceeding 1.0 is clamped to 1.0")
        void exceedingValue_ClampedToOne() {
            assertThat(scoringService.calculateOwnershipConcentrationScore(1.25)).isEqualTo(1.0);
        }

        @Test
        @DisplayName("NaN ownership concentration is clamped to 0.0")
        void nanValue_ClampedToZero() {
            assertThat(scoringService.calculateOwnershipConcentrationScore(Double.NaN)).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Composite and Baseline Scoring Tests")
    class CompositeAndBaselineTests {

        @Test
        @DisplayName("Composite score computes exact weighted sum")
        void compositeScore_ExactWeightedSum() {
            // Set up metrics such that:
            // revisionFrequencyScore = 1.0 (50 / 50)
            // churnScore = 1.0 (500 / 500)
            // recencyScore = 0.5 (90 days ago)
            // ownershipConcentrationScore = 0.5 (0.50)
            // Expected composite = 0.30(1.0) + 0.30(1.0) + 0.20(0.5) + 0.20(0.5) = 0.80
            FileRiskInput input = new FileRiskInput(
                    50,
                    500,
                    referenceTime.minus(Duration.ofDays(90)),
                    0.50
            );
            FileRiskNormalizationContext context = new FileRiskNormalizationContext(50, 500);

            FileRiskScore score = scoringService.scoreFile(input, context, referenceTime);

            assertThat(score.revisionFrequencyScore()).isEqualTo(1.0);
            assertThat(score.churnScore()).isEqualTo(1.0);
            assertThat(score.recencyScore()).isCloseTo(0.5, within(1e-9));
            assertThat(score.ownershipConcentrationScore()).isEqualTo(0.50);
            assertThat(score.compositeScore()).isCloseTo(0.80, within(1e-9));
        }

        @Test
        @DisplayName("Baseline score strictly equals revision frequency score")
        void baselineScore_StrictlyEqualsRevisionFrequencyScore() {
            FileRiskInput input = new FileRiskInput(
                    25,
                    300,
                    referenceTime.minus(Duration.ofDays(30)),
                    0.80
            );
            FileRiskNormalizationContext context = new FileRiskNormalizationContext(100, 1000);

            FileRiskScore score = scoringService.scoreFile(input, context, referenceTime);

            assertThat(score.baselineScore()).isEqualTo(score.revisionFrequencyScore());
        }

        @Test
        @DisplayName("Deterministic execution: identical inputs produce identical scores")
        void deterministicExecution() {
            FileRiskInput input = new FileRiskInput(15, 200, referenceTime.minus(Duration.ofDays(45)), 0.65);
            FileRiskNormalizationContext context = new FileRiskNormalizationContext(80, 500);

            FileRiskScore score1 = scoringService.scoreFile(input, context, referenceTime);
            FileRiskScore score2 = scoringService.scoreFile(input, context, referenceTime);

            assertThat(score1).isEqualTo(score2);
            assertThat(score1.compositeScore()).isEqualTo(score2.compositeScore());
        }

        @Test
        @DisplayName("Boundary verification: all output scores stay strictly within [0.0, 1.0]")
        void allScoresWithinZeroAndOne() {
            FileRiskInput input = new FileRiskInput(999999, 999999, referenceTime.plus(Duration.ofDays(100)), 2.0);
            FileRiskNormalizationContext context = new FileRiskNormalizationContext(10, 10);

            FileRiskScore score = scoringService.scoreFile(input, context, referenceTime);

            assertThat(score.baselineScore()).isBetween(0.0, 1.0);
            assertThat(score.revisionFrequencyScore()).isBetween(0.0, 1.0);
            assertThat(score.churnScore()).isBetween(0.0, 1.0);
            assertThat(score.recencyScore()).isBetween(0.0, 1.0);
            assertThat(score.ownershipConcentrationScore()).isBetween(0.0, 1.0);
            assertThat(score.compositeScore()).isBetween(0.0, 1.0);
        }

        @Test
        @DisplayName("Null arguments throw NullPointerException")
        void nullArguments_ThrowException() {
            FileRiskInput input = new FileRiskInput(10, 100, referenceTime, 0.5);
            FileRiskNormalizationContext context = new FileRiskNormalizationContext(10, 100);

            assertThatThrownBy(() -> scoringService.scoreFile(null, context, referenceTime))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("input must not be null");

            assertThatThrownBy(() -> scoringService.scoreFile(input, null, referenceTime))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("context must not be null");

            assertThatThrownBy(() -> scoringService.scoreFile(input, context, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("referenceTime must not be null");
        }
    }
}