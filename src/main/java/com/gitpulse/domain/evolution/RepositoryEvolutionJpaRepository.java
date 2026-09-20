package com.gitpulse.domain.evolution;

import com.gitpulse.domain.commit.Commit;
import com.gitpulse.domain.evolution.dto.RepositoryMonthlyEvolutionRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface RepositoryEvolutionJpaRepository extends JpaRepository<Commit, Long> {

    @Query(value = """
            WITH months AS (
                SELECT s.bucket_month
                FROM generate_series(
                    date_trunc('month', CAST(:from AS TIMESTAMP WITH TIME ZONE)),
                    date_trunc('month', CAST(:to AS TIMESTAMP WITH TIME ZONE)),
                    INTERVAL '1' MONTH
                ) AS s(bucket_month)
                WHERE s.bucket_month < CAST(:to AS TIMESTAMP WITH TIME ZONE)
            ),
            commit_agg AS (
                SELECT
                    date_trunc('month', c.committed_at) AS commit_month,
                    COUNT(c.id) AS total_commits,
                    COALESCE(SUM(c.additions), 0) AS total_additions,
                    COALESCE(SUM(c.deletions), 0) AS total_deletions,
                    COALESCE(SUM(COALESCE(c.additions, 0) + COALESCE(c.deletions, 0)), 0) AS total_churn,
                    COUNT(DISTINCT CASE WHEN c.author_email IS NOT NULL AND TRIM(c.author_email) <> '' THEN LOWER(TRIM(c.author_email)) END) AS active_contributors,
                    COUNT(CASE WHEN c.classification = 'FEATURE' THEN 1 END) AS feature_commits,
                    COUNT(CASE WHEN c.classification = 'BUG_FIX' THEN 1 END) AS bug_fix_commits,
                    COUNT(CASE WHEN c.classification = 'REFACTOR' THEN 1 END) AS refactor_commits,
                    COUNT(CASE WHEN c.classification = 'DOCUMENTATION' THEN 1 END) AS documentation_commits,
                    COUNT(CASE WHEN c.classification = 'TEST' THEN 1 END) AS test_commits,
                    COUNT(CASE WHEN c.classification = 'BUILD' THEN 1 END) AS build_commits,
                    COUNT(CASE WHEN c.classification = 'CONFIGURATION' THEN 1 END) AS configuration_commits,
                    COUNT(CASE WHEN c.classification = 'DEPENDENCY' THEN 1 END) AS dependency_commits,
                    COUNT(CASE WHEN c.classification = 'OTHER' THEN 1 END) AS other_commits
                FROM commits c
                WHERE c.repository_id = :repositoryId
                  AND c.committed_at >= :from
                  AND c.committed_at < :to
                GROUP BY date_trunc('month', c.committed_at)
            ),
            file_agg AS (
                SELECT
                    date_trunc('month', c.committed_at) AS file_month,
                    COUNT(DISTINCT fc.file_path) AS files_changed
                FROM commits c
                JOIN file_changes fc ON fc.commit_id = c.id
                WHERE c.repository_id = :repositoryId
                  AND c.committed_at >= :from
                  AND c.committed_at < :to
                GROUP BY date_trunc('month', c.committed_at)
            )
            SELECT
                m.bucket_month AS bucketMonth,
                CAST(COALESCE(ca.total_commits, 0) AS BIGINT) AS totalCommits,
                CAST(COALESCE(ca.total_additions, 0) AS BIGINT) AS totalAdditions,
                CAST(COALESCE(ca.total_deletions, 0) AS BIGINT) AS totalDeletions,
                CAST(COALESCE(ca.total_churn, 0) AS BIGINT) AS totalChurn,
                CAST(COALESCE(ca.active_contributors, 0) AS BIGINT) AS activeContributors,
                CAST(COALESCE(fa.files_changed, 0) AS BIGINT) AS filesChanged,
                CAST(COALESCE(ca.feature_commits, 0) AS BIGINT) AS featureCommits,
                CAST(COALESCE(ca.bug_fix_commits, 0) AS BIGINT) AS bugFixCommits,
                CAST(COALESCE(ca.refactor_commits, 0) AS BIGINT) AS refactorCommits,
                CAST(COALESCE(ca.documentation_commits, 0) AS BIGINT) AS documentationCommits,
                CAST(COALESCE(ca.test_commits, 0) AS BIGINT) AS testCommits,
                CAST(COALESCE(ca.build_commits, 0) AS BIGINT) AS buildCommits,
                CAST(COALESCE(ca.configuration_commits, 0) AS BIGINT) AS configurationCommits,
                CAST(COALESCE(ca.dependency_commits, 0) AS BIGINT) AS dependencyCommits,
                CAST(COALESCE(ca.other_commits, 0) AS BIGINT) AS otherCommits
            FROM months m
            LEFT JOIN commit_agg ca ON m.bucket_month = ca.commit_month
            LEFT JOIN file_agg fa ON m.bucket_month = fa.file_month
            ORDER BY m.bucket_month ASC
            """, nativeQuery = true)
    List<RepositoryMonthlyEvolutionRow> findMonthlyEvolution(
            @Param("repositoryId") Long repositoryId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query(value = """
            WITH commit_agg AS (
                SELECT
                    COUNT(c.id) AS total_commits,
                    COALESCE(SUM(c.additions), 0) AS total_additions,
                    COALESCE(SUM(c.deletions), 0) AS total_deletions,
                    COALESCE(SUM(COALESCE(c.additions, 0) + COALESCE(c.deletions, 0)), 0) AS total_churn,
                    COUNT(DISTINCT CASE WHEN c.author_email IS NOT NULL AND TRIM(c.author_email) <> '' THEN LOWER(TRIM(c.author_email)) END) AS active_contributors,
                    COUNT(CASE WHEN c.classification = 'FEATURE' THEN 1 END) AS feature_commits,
                    COUNT(CASE WHEN c.classification = 'BUG_FIX' THEN 1 END) AS bug_fix_commits,
                    COUNT(CASE WHEN c.classification = 'REFACTOR' THEN 1 END) AS refactor_commits,
                    COUNT(CASE WHEN c.classification = 'DOCUMENTATION' THEN 1 END) AS documentation_commits,
                    COUNT(CASE WHEN c.classification = 'TEST' THEN 1 END) AS test_commits,
                    COUNT(CASE WHEN c.classification = 'BUILD' THEN 1 END) AS build_commits,
                    COUNT(CASE WHEN c.classification = 'CONFIGURATION' THEN 1 END) AS configuration_commits,
                    COUNT(CASE WHEN c.classification = 'DEPENDENCY' THEN 1 END) AS dependency_commits,
                    COUNT(CASE WHEN c.classification = 'OTHER' THEN 1 END) AS other_commits
                FROM commits c
                WHERE c.repository_id = :repositoryId
                  AND c.committed_at >= :from
                  AND c.committed_at < :to
            ),
            file_agg AS (
                SELECT
                    COUNT(DISTINCT fc.file_path) AS files_changed
                FROM commits c
                JOIN file_changes fc ON fc.commit_id = c.id
                WHERE c.repository_id = :repositoryId
                  AND c.committed_at >= :from
                  AND c.committed_at < :to
            )
            SELECT
                CAST(COALESCE(ca.total_commits, 0) AS BIGINT) AS totalCommits,
                CAST(COALESCE(ca.total_additions, 0) AS BIGINT) AS totalAdditions,
                CAST(COALESCE(ca.total_deletions, 0) AS BIGINT) AS totalDeletions,
                CAST(COALESCE(ca.total_churn, 0) AS BIGINT) AS totalChurn,
                CAST(COALESCE(ca.active_contributors, 0) AS BIGINT) AS activeContributors,
                CAST(COALESCE(fa.files_changed, 0) AS BIGINT) AS filesChanged,
                CAST(COALESCE(ca.feature_commits, 0) AS BIGINT) AS featureCommits,
                CAST(COALESCE(ca.bug_fix_commits, 0) AS BIGINT) AS bugFixCommits,
                CAST(COALESCE(ca.refactor_commits, 0) AS BIGINT) AS refactorCommits,
                CAST(COALESCE(ca.documentation_commits, 0) AS BIGINT) AS documentationCommits,
                CAST(COALESCE(ca.test_commits, 0) AS BIGINT) AS testCommits,
                CAST(COALESCE(ca.build_commits, 0) AS BIGINT) AS buildCommits,
                CAST(COALESCE(ca.configuration_commits, 0) AS BIGINT) AS configurationCommits,
                CAST(COALESCE(ca.dependency_commits, 0) AS BIGINT) AS dependencyCommits,
                CAST(COALESCE(ca.other_commits, 0) AS BIGINT) AS otherCommits
            FROM commit_agg ca
            CROSS JOIN file_agg fa
            """, nativeQuery = true)
    com.gitpulse.domain.evolution.dto.RepositoryEvolutionPeriodSummaryRow findPeriodSummary(
            @Param("repositoryId") Long repositoryId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );
}
