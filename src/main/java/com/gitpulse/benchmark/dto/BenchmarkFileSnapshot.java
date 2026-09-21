package com.gitpulse.benchmark.dto;

import com.gitpulse.domain.risk.FileRiskScore;

import java.time.Instant;

/**
 * Immutable record representing a file's state and analytical scores at historical cutoff time T_cutoff.
 */
public record BenchmarkFileSnapshot(
        String filePath,
        long historicalRevisions,
        long historicalChurn,
        Instant historicalLastModifiedAt,
        double historicalTopContributorShare,
        FileRiskScore scores
) {
}
