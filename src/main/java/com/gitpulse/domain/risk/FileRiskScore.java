package com.gitpulse.domain.risk;

/**
 * Immutable output model containing normalized analytical risk scores for a file.
 * All individual and composite scores are strictly within the range [0.0, 1.0].
 *
 * @param baselineScore               baseline single-dimensional risk score (identical to revisionFrequencyScore)
 * @param revisionFrequencyScore      log-normalized revision frequency score relative to repository maximum
 * @param churnScore                  log-normalized code churn score relative to repository maximum
 * @param recencyScore                exponential decay recency score based on a true 90-day half-life
 * @param ownershipConcentrationScore descriptive concentration score of file modifications in top contributor
 * @param compositeScore              weighted multi-dimensional composite score
 */
public record FileRiskScore(
        double baselineScore,
        double revisionFrequencyScore,
        double churnScore,
        double recencyScore,
        double ownershipConcentrationScore,
        double compositeScore
) {
}