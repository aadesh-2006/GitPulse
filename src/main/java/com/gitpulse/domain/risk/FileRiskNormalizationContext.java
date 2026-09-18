package com.gitpulse.domain.risk;

/**
 * Immutable context containing repository-level reference maxima required to normalize
 * file-level revisions and churn using log normalization.
 *
 * @param repositoryMaxRevisions maximum total revisions across all files in the repository
 * @param repositoryMaxChurn     maximum total churn across all files in the repository
 */
public record FileRiskNormalizationContext(
        long repositoryMaxRevisions,
        long repositoryMaxChurn
) {
}