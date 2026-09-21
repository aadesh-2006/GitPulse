package com.gitpulse.benchmark.repository;

import com.gitpulse.benchmark.dto.FutureFileOutcomeRow;
import com.gitpulse.benchmark.dto.HistoricalAuthorFileCountRow;
import com.gitpulse.benchmark.dto.HistoricalFileSnapshotRow;
import com.gitpulse.domain.filechange.FileChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Isolated query repository for temporal repository evaluation without reading mutable present-day read models.
 */
@Repository
public interface BenchmarkQueryRepository extends JpaRepository<FileChange, Long> {

    @Query(value = """
            SELECT
                f.file_path AS filePath,
                COUNT(f.id) AS totalRevisions,
                COALESCE(SUM(COALESCE(f.changes, COALESCE(f.additions, 0) + COALESCE(f.deletions, 0))), 0) AS totalChurn,
                MAX(c.committed_at) AS lastModifiedAt
            FROM file_changes f
            JOIN commits c ON f.commit_id = c.id
            WHERE c.repository_id = :repositoryId
              AND c.committed_at <= :cutoffTime
            GROUP BY f.file_path
            """, nativeQuery = true)
    List<HistoricalFileSnapshotRow> findHistoricalFileSnapshots(
            @Param("repositoryId") Long repositoryId,
            @Param("cutoffTime") Instant cutoffTime
    );

    @Query(value = """
            SELECT
                f.file_path AS filePath,
                COALESCE(LOWER(TRIM(c.author_email)), LOWER(TRIM(c.author_username)), LOWER(TRIM(c.author_name)), 'unknown') AS authorKey,
                COUNT(f.id) AS revisionCount
            FROM file_changes f
            JOIN commits c ON f.commit_id = c.id
            WHERE c.repository_id = :repositoryId
              AND c.committed_at <= :cutoffTime
            GROUP BY f.file_path, COALESCE(LOWER(TRIM(c.author_email)), LOWER(TRIM(c.author_username)), LOWER(TRIM(c.author_name)), 'unknown')
            """, nativeQuery = true)
    List<HistoricalAuthorFileCountRow> findHistoricalAuthorFileCounts(
            @Param("repositoryId") Long repositoryId,
            @Param("cutoffTime") Instant cutoffTime
    );

    @Query(value = """
            SELECT
                f.file_path AS filePath,
                COUNT(f.id) AS futureRevisionCount,
                COALESCE(SUM(COALESCE(f.changes, COALESCE(f.additions, 0) + COALESCE(f.deletions, 0))), 0) AS futureChurn,
                COUNT(DISTINCT COALESCE(LOWER(TRIM(c.author_email)), LOWER(TRIM(c.author_username)), LOWER(TRIM(c.author_name)), 'unknown')) AS futureDistinctContributors
            FROM file_changes f
            JOIN commits c ON f.commit_id = c.id
            WHERE c.repository_id = :repositoryId
              AND c.committed_at > :cutoffTime
              AND c.committed_at <= :horizonEndTime
            GROUP BY f.file_path
            """, nativeQuery = true)
    List<FutureFileOutcomeRow> findFutureFileOutcomes(
            @Param("repositoryId") Long repositoryId,
            @Param("cutoffTime") Instant cutoffTime,
            @Param("horizonEndTime") Instant horizonEndTime
    );
}
