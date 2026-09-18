package com.gitpulse.domain.risk;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Deterministic and stateless scoring engine for file-level risk and stability intelligence.
 * <p>
 * Evaluates multi-dimensional historical change signals:
 * <ul>
 *   <li><b>Revision Frequency</b>: Log-normalized against repository maximum (weight = 0.30)</li>
 *   <li><b>Code Churn</b>: Log-normalized lines added/deleted against repository maximum (weight = 0.30)</li>
 *   <li><b>Recency</b>: Exponential decay with a true 90-day half-life: {@code exp(-ln(2) * ageDays / 90)} (weight = 0.20)</li>
 *   <li><b>Ownership Concentration</b>: Top contributor historical revision share clamped to [0.0, 1.0] (weight = 0.20)</li>
 * </ul>
 * <p>
 * Also provides a single-dimensional {@code baselineScore} identical to {@code revisionFrequencyScore}
 * to enable baseline vs multi-dimensional comparative research.
 */
@Service
public class FileRiskScoringService {

    public static final double WEIGHT_REVISION_FREQUENCY = 0.30;
    public static final double WEIGHT_CHURN = 0.30;
    public static final double WEIGHT_RECENCY = 0.20;
    public static final double WEIGHT_OWNERSHIP_CONCENTRATION = 0.20;

    public static final double HALF_LIFE_DAYS = 90.0;
    private static final double LN_2 = Math.log(2.0);
    private static final double SECONDS_PER_DAY = 86_400.0;
    private static final double NANOS_PER_DAY = 86_400_000_000_000.0;

    static {
        double weightSum = WEIGHT_REVISION_FREQUENCY + WEIGHT_CHURN + WEIGHT_RECENCY + WEIGHT_OWNERSHIP_CONCENTRATION;
        if (Math.abs(weightSum - 1.0) > 1e-9) {
            throw new IllegalStateException("Scoring weights must sum to 1.0, current sum: " + weightSum);
        }
    }

    /**
     * Scores a file's risk and stability metrics against repository-level normalization context.
     *
     * @param input         file-level metrics, must not be null
     * @param context       repository-level normalization context, must not be null
     * @param referenceTime reference timestamp for recency evaluation, must not be null
     * @return the calculated {@link FileRiskScore} with all metrics normalized to [0.0, 1.0]
     */
    public FileRiskScore scoreFile(
            FileRiskInput input,
            FileRiskNormalizationContext context,
            Instant referenceTime
    ) {
        Objects.requireNonNull(input, "input must not be null");
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(referenceTime, "referenceTime must not be null");

        double revisionFrequencyScore = calculateRevisionFrequencyScore(input.totalRevisions(), context.repositoryMaxRevisions());
        double churnScore = calculateChurnScore(input.totalChurn(), context.repositoryMaxChurn());
        double recencyScore = calculateRecencyScore(input.lastModifiedAt(), referenceTime);
        double ownershipConcentrationScore = calculateOwnershipConcentrationScore(input.topContributorRevisionShare());

        double baselineScore = revisionFrequencyScore;

        double compositeScore = clamp(
                WEIGHT_REVISION_FREQUENCY * revisionFrequencyScore
                        + WEIGHT_CHURN * churnScore
                        + WEIGHT_RECENCY * recencyScore
                        + WEIGHT_OWNERSHIP_CONCENTRATION * ownershipConcentrationScore
        );

        return new FileRiskScore(
                baselineScore,
                revisionFrequencyScore,
                churnScore,
                recencyScore,
                ownershipConcentrationScore,
                compositeScore
        );
    }

    /**
     * Calculates log-normalized revision frequency score: {@code log1p(totalRevisions) / log1p(repositoryMaxRevisions)}.
     *
     * @param totalRevisions          total revisions for the file
     * @param repositoryMaxRevisions maximum revisions across any file in the repository
     * @return normalized score in [0.0, 1.0]
     */
    public double calculateRevisionFrequencyScore(long totalRevisions, long repositoryMaxRevisions) {
        if (totalRevisions <= 0 || repositoryMaxRevisions <= 0) {
            return 0.0;
        }
        double score = Math.log1p((double) totalRevisions) / Math.log1p((double) repositoryMaxRevisions);
        return clamp(score);
    }

    /**
     * Calculates log-normalized churn score: {@code log1p(totalChurn) / log1p(repositoryMaxChurn)}.
     *
     * @param totalChurn          total churn for the file
     * @param repositoryMaxChurn maximum churn across any file in the repository
     * @return normalized score in [0.0, 1.0]
     */
    public double calculateChurnScore(long totalChurn, long repositoryMaxChurn) {
        if (totalChurn <= 0 || repositoryMaxChurn <= 0) {
            return 0.0;
        }
        double score = Math.log1p((double) totalChurn) / Math.log1p((double) repositoryMaxChurn);
        return clamp(score);
    }

    /**
     * Calculates exponential recency decay score with a true 90-day half-life:
     * {@code exp(-ln(2) * ageDays / 90.0)}.
     *
     * @param lastModifiedAt timestamp when file was last modified (null yields 0.0)
     * @param referenceTime  reference evaluation timestamp, must not be null
     * @return normalized score in [0.0, 1.0]
     */
    public double calculateRecencyScore(Instant lastModifiedAt, Instant referenceTime) {
        Objects.requireNonNull(referenceTime, "referenceTime must not be null");
        if (lastModifiedAt == null) {
            return 0.0;
        }
        if (lastModifiedAt.isAfter(referenceTime)) {
            return 1.0;
        }

        Duration duration = Duration.between(lastModifiedAt, referenceTime);
        double ageDays = (duration.getSeconds() / SECONDS_PER_DAY)
                + (duration.getNano() / NANOS_PER_DAY);
        if (ageDays <= 0.0) {
            return 1.0;
        }

        double score = Math.exp(-LN_2 * ageDays / HALF_LIFE_DAYS);
        return clamp(score);
    }

    /**
     * Calculates ownership concentration score by clamping the top contributor revision share to [0.0, 1.0].
     *
     * @param topContributorRevisionShare fraction of revisions by the top contributor
     * @return normalized score in [0.0, 1.0]
     */
    public double calculateOwnershipConcentrationScore(double topContributorRevisionShare) {
        return clamp(topContributorRevisionShare);
    }

    private static double clamp(double value) {
        if (Double.isNaN(value) || value <= 0.0) {
            return 0.0;
        }
        if (value >= 1.0) {
            return 1.0;
        }
        return value;
    }
}