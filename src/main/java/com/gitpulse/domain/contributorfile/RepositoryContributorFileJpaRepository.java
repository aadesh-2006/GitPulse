package com.gitpulse.domain.contributorfile;

import com.gitpulse.domain.contributorfile.dto.RepositoryContributorFileAggregationRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepositoryContributorFileJpaRepository extends JpaRepository<RepositoryContributorFile, Long> {

    @EntityGraph(attributePaths = {"contributor"})
    List<RepositoryContributorFile> findByRepositoryId(Long repositoryId);

    @EntityGraph(attributePaths = {"contributor"})
    Page<RepositoryContributorFile> findByRepositoryId(Long repositoryId, Pageable pageable);

    @EntityGraph(attributePaths = {"contributor"})
    Optional<RepositoryContributorFile> findByRepositoryIdAndContributorIdAndFilePath(Long repositoryId, Long contributorId, String filePath);

    @EntityGraph(attributePaths = {"contributor"})
    List<RepositoryContributorFile> findByRepositoryIdAndContributorId(Long repositoryId, Long contributorId);

    @EntityGraph(attributePaths = {"contributor"})
    Page<RepositoryContributorFile> findByRepositoryIdAndContributorId(Long repositoryId, Long contributorId, Pageable pageable);

    @EntityGraph(attributePaths = {"contributor"})
    List<RepositoryContributorFile> findByRepositoryIdAndFilePath(Long repositoryId, String filePath);

    @EntityGraph(attributePaths = {"contributor"})
    Page<RepositoryContributorFile> findByRepositoryIdAndFilePath(Long repositoryId, String filePath, Pageable pageable);

    void deleteByRepositoryId(Long repositoryId);

    long countByRepositoryId(Long repositoryId);

    @EntityGraph(attributePaths = {"contributor"})
    @Query("""
            SELECT rcf FROM RepositoryContributorFile rcf
            WHERE rcf.repository.id = :repositoryId
              AND CONCAT(rcf.contributor.id, ':::', rcf.filePath) IN :compositeKeys
            """)
    List<RepositoryContributorFile> findByRepositoryIdAndCompositeKeys(
            @Param("repositoryId") Long repositoryId,
            @Param("compositeKeys") java.util.Collection<String> compositeKeys
    );

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = """
            DELETE FROM repository_contributor_files
            WHERE repository_id = :repositoryId
              AND NOT EXISTS (
                  SELECT 1
                  FROM file_changes f
                  JOIN commits c ON f.commit_id = c.id
                  JOIN contributors contrib ON LOWER(TRIM(c.author_email)) = LOWER(TRIM(contrib.email))
                  WHERE c.repository_id = :repositoryId
                    AND c.author_email IS NOT NULL
                    AND TRIM(c.author_email) <> ''
                    AND contrib.id = repository_contributor_files.contributor_id
                    AND f.file_path = repository_contributor_files.file_path
              )
            """, nativeQuery = true)
    int deleteStaleContributorFilesByRepositoryId(@Param("repositoryId") Long repositoryId);

    @Query(value = """
            SELECT 
                contrib.id AS contributorId,
                f.file_path AS filePath,
                COUNT(f.id) AS totalRevisions,
                COALESCE(SUM(f.additions), 0) AS totalAdditions,
                COALESCE(SUM(f.deletions), 0) AS totalDeletions,
                COALESCE(SUM(f.changes), 0) AS totalChurn,
                MIN(c.committed_at) AS firstContributedAt,
                MAX(c.committed_at) AS lastContributedAt
            FROM file_changes f
            JOIN commits c ON f.commit_id = c.id
            JOIN contributors contrib ON LOWER(TRIM(c.author_email)) = LOWER(TRIM(contrib.email))
            WHERE c.repository_id = :repositoryId
              AND c.author_email IS NOT NULL
              AND TRIM(c.author_email) <> ''
            GROUP BY contrib.id, f.file_path
            ORDER BY contrib.id ASC, f.file_path ASC
            """,
            countQuery = """
            SELECT COUNT(*) FROM (
                SELECT 1
                FROM file_changes f
                JOIN commits c ON f.commit_id = c.id
                JOIN contributors contrib ON LOWER(TRIM(c.author_email)) = LOWER(TRIM(contrib.email))
                WHERE c.repository_id = :repositoryId
                  AND c.author_email IS NOT NULL
                  AND TRIM(c.author_email) <> ''
                GROUP BY contrib.id, f.file_path
            ) AS agg_count
            """,
            nativeQuery = true)
    Page<RepositoryContributorFileAggregationRow> aggregateContributorFilesByRepositoryId(
            @Param("repositoryId") Long repositoryId,
            Pageable pageable
    );
}
