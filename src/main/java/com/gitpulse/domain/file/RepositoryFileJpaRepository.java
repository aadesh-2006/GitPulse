package com.gitpulse.domain.file;

import com.gitpulse.domain.file.dto.FilePrimaryContributorRow;
import com.gitpulse.domain.file.dto.RepositoryFileAggregationRow;
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
public interface RepositoryFileJpaRepository extends JpaRepository<RepositoryFile, Long> {

    @EntityGraph(attributePaths = {"primaryContributor"})
    Optional<RepositoryFile> findByRepositoryIdAndFilePath(Long repositoryId, String filePath);

    @EntityGraph(attributePaths = {"primaryContributor"})
    List<RepositoryFile> findByRepositoryId(Long repositoryId);

    @EntityGraph(attributePaths = {"primaryContributor"})
    Page<RepositoryFile> findByRepositoryId(Long repositoryId, Pageable pageable);

    @EntityGraph(attributePaths = {"primaryContributor"})
    @Query("""
            SELECT rf FROM RepositoryFile rf
            WHERE rf.repository.id = :repositoryId
              AND (:extension IS NULL OR rf.extension = :extension)
              AND (:isDeleted IS NULL OR rf.isDeleted = :isDeleted)
            """)
    Page<RepositoryFile> findByRepositoryIdWithFilters(
            @Param("repositoryId") Long repositoryId,
            @Param("extension") String extension,
            @Param("isDeleted") Boolean isDeleted,
            Pageable pageable
    );

    @Query(value = """
            SELECT 
                f.file_path AS filePath,
                COUNT(f.id) AS totalRevisions,
                COALESCE(SUM(f.additions), 0) AS totalAdditions,
                COALESCE(SUM(f.deletions), 0) AS totalDeletions,
                COALESCE(SUM(f.changes), 0) AS totalChurn,
                MIN(c.committed_at) AS firstModifiedAt,
                MAX(c.committed_at) AS lastModifiedAt,
                (
                    SELECT f2.status 
                    FROM file_changes f2 
                    JOIN commits c2 ON f2.commit_id = c2.id 
                    WHERE c2.repository_id = :repositoryId 
                      AND f2.file_path = f.file_path 
                    ORDER BY c2.committed_at DESC, c2.id DESC 
                    LIMIT 1
                ) AS lastStatus
            FROM file_changes f
            JOIN commits c ON f.commit_id = c.id
            WHERE c.repository_id = :repositoryId
            GROUP BY f.file_path
            """, nativeQuery = true)
    List<RepositoryFileAggregationRow> aggregateFilesByRepositoryId(@Param("repositoryId") Long repositoryId);

    @Query(value = """
            SELECT 
                f.file_path AS filePath,
                contrib.id AS contributorId,
                COUNT(f.id) AS contributionCount,
                MAX(c.committed_at) AS latestContributionAt
            FROM file_changes f
            JOIN commits c ON f.commit_id = c.id
            JOIN contributors contrib ON LOWER(TRIM(c.author_email)) = LOWER(TRIM(contrib.email))
            WHERE c.repository_id = :repositoryId
            GROUP BY f.file_path, contrib.id
            ORDER BY f.file_path ASC, COUNT(f.id) DESC, MAX(c.committed_at) DESC, contrib.id ASC
            """, nativeQuery = true)
    List<FilePrimaryContributorRow> findPrimaryContributorsByRepositoryId(@Param("repositoryId") Long repositoryId);

    @Query("""
            SELECT
                COALESCE(MAX(rf.totalRevisions), 0) AS maxRevisions,
                COALESCE(MAX(rf.totalChurn), 0) AS maxChurn
            FROM RepositoryFile rf
            WHERE rf.repository.id = :repositoryId
            """)
    com.gitpulse.domain.file.dto.RepositoryFileNormalizationMaximaRow findNormalizationMaximaByRepositoryId(@Param("repositoryId") Long repositoryId);
}