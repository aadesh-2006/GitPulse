package com.gitpulse.benchmark.dto;

import java.util.Objects;

/**
 * Immutable evaluation item pairing a file's historical snapshot with its future evaluation outcome.
 */
public record BenchmarkEvaluationItem(
        BenchmarkFileSnapshot snapshot,
        BenchmarkFutureOutcome outcome
) {
    public BenchmarkEvaluationItem {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(outcome, "outcome must not be null");
    }

    public String filePath() {
        return snapshot.filePath();
    }

    public boolean futureChanged() {
        return outcome.futureChanged();
    }

    public long futureRevisionCount() {
        return outcome.futureRevisionCount();
    }

    public long futureChurn() {
        return outcome.futureChurn();
    }
}
