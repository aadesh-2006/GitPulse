package com.gitpulse.benchmark.analysis;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Deterministic temporal cutoff timestamp selector.
 * <p>
 * Given a repository's historical span [T_earliest, T_latest] and a required future observation horizon,
 * calculates deterministic, evenly-spaced cutoff timestamps such that:
 * <ol>
 *   <li>Each cutoff has sufficient historical depth (at least one horizon duration prior to cutoff)</li>
 *   <li>Each cutoff has a full future observation horizon before T_latest: {@code cutoff + horizon <= T_latest}</li>
 *   <li>Cutoffs are strictly ordered and non-overlapping in time</li>
 * </ol>
 */
public final class BenchmarkCutoffSelector {

    private BenchmarkCutoffSelector() {
        // Utility class
    }

    /**
     * Deterministically computes {@code numCutoffs} timestamps within the valid history interval.
     *
     * @param earliestCommit  timestamp of the earliest commit in repository, must not be null
     * @param latestCommit    timestamp of the latest commit in repository, must not be null
     * @param horizonDuration duration of future observation window (e.g., 90 days), must not be null
     * @param numCutoffs      number of cutoffs to generate (typically 3)
     * @return sorted list of valid historical cutoff timestamps, or empty list if history is insufficient
     */
    public static List<Instant> selectCutoffs(
            Instant earliestCommit,
            Instant latestCommit,
            Duration horizonDuration,
            int numCutoffs
    ) {
        Objects.requireNonNull(earliestCommit, "earliestCommit must not be null");
        Objects.requireNonNull(latestCommit, "latestCommit must not be null");
        Objects.requireNonNull(horizonDuration, "horizonDuration must not be null");

        if (numCutoffs <= 0 || horizonDuration.isNegative() || horizonDuration.isZero()) {
            return Collections.emptyList();
        }

        // Usable interval: [earliestCommit + horizon, latestCommit - horizon]
        Instant minValidCutoff = earliestCommit.plus(horizonDuration);
        Instant maxValidCutoff = latestCommit.minus(horizonDuration);

        if (!minValidCutoff.isBefore(maxValidCutoff)) {
            // Not enough historical depth for multi-cutoff evaluation with the specified horizon
            return Collections.emptyList();
        }

        List<Instant> cutoffs = new ArrayList<>(numCutoffs);
        if (numCutoffs == 1) {
            long midpointEpochSec = (minValidCutoff.getEpochSecond() + maxValidCutoff.getEpochSecond()) / 2;
            cutoffs.add(Instant.ofEpochSecond(midpointEpochSec));
            return cutoffs;
        }

        long totalSpanSeconds = maxValidCutoff.getEpochSecond() - minValidCutoff.getEpochSecond();
        long stepSeconds = totalSpanSeconds / (numCutoffs - 1);

        for (int i = 0; i < numCutoffs; i++) {
            long cutoffEpochSec = minValidCutoff.getEpochSecond() + (i * stepSeconds);
            cutoffs.add(Instant.ofEpochSecond(cutoffEpochSec));
        }

        return Collections.unmodifiableList(cutoffs);
    }
}
