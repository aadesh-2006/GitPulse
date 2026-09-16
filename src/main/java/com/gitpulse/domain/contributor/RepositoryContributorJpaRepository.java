package com.gitpulse.domain.contributor;

import com.gitpulse.domain.contributor.dto.ContributorAggregationRow;
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
public interface RepositoryContributorJpaRepository extends JpaRepository<RepositoryContributor, Long> {

    @EntityGraph(attributePaths = {"contributor"})
    Page<RepositoryContributor> findByRepositoryId(Long repositoryId, Pageable pageable);

    @EntityGraph(attributePaths = {"contributor"})
    Optional<RepositoryContributor> findByRepositoryIdAndContributorId(Long repositoryId, Long contributorId);

    @EntityGraph(attributePaths = {"contributor"})
    List<RepositoryContributor> findByRepositoryId(Long repositoryId);

    long countByRepositoryId(Long repositoryId);

    @Query(value = """
            SELECT 
                LOWER(TRIM(c.author_email)) AS email,
                (SELECT c2.author_username FROM commits c2 
                 WHERE c2.repository_id = :repositoryId 
                   AND LOWER(TRIM(c2.author_email)) = LOWER(TRIM(c.author_email)) 
                   AND c2.author_username IS NOT NULL AND TRIM(c2.author_username) <> '' 
                 ORDER BY c2.committed_at DESC, c2.id DESC LIMIT 1) AS username,
                (SELECT c3.author_name FROM commits c3 
                 WHERE c3.repository_id = :repositoryId 
                   AND LOWER(TRIM(c3.author_email)) = LOWER(TRIM(c.author_email)) 
                   AND c3.author_name IS NOT NULL AND TRIM(c3.author_name) <> '' 
                 ORDER BY c3.committed_at DESC, c3.id DESC LIMIT 1) AS name,
                CAST(COUNT(c.id) AS INTEGER) AS totalCommits,
                CAST(COALESCE(SUM(c.additions), 0) AS INTEGER) AS totalAdditions,
                CAST(COALESCE(SUM(c.deletions), 0) AS INTEGER) AS totalDeletions,
                CAST(COALESCE(SUM(c.total_changes), 0) AS INTEGER) AS totalChanges,
                MIN(c.committed_at) AS firstCommittedAt,
                MAX(c.committed_at) AS lastCommittedAt
            FROM commits c
            WHERE c.repository_id = :repositoryId 
              AND c.author_email IS NOT NULL 
              AND TRIM(c.author_email) <> ''
            GROUP BY LOWER(TRIM(c.author_email))
            """, nativeQuery = true)
    List<ContributorAggregationRow> aggregateContributorsByRepositoryId(@Param("repositoryId") Long repositoryId);
}
