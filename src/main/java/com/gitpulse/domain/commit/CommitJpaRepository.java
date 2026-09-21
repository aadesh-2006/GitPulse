package com.gitpulse.domain.commit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommitJpaRepository extends JpaRepository<Commit, Long> {

    boolean existsByRepositoryIdAndGithubCommitSha(Long repositoryId, String githubCommitSha);

    @Query("SELECT c.githubCommitSha FROM Commit c WHERE c.repository.id = :repositoryId AND c.githubCommitSha IN :shas")
    List<String> findExistingGithubCommitShas(@Param("repositoryId") Long repositoryId, @Param("shas") Collection<String> shas);

    long countByRepositoryId(Long repositoryId);

    Page<Commit> findByRepositoryIdOrderByCommittedAtDesc(Long repositoryId, Pageable pageable);

    Page<Commit> findByRepositoryIdOrderByIdAsc(Long repositoryId, Pageable pageable);

    @Query("SELECT c FROM Commit c WHERE c.repository.id = :repositoryId AND c.classification IS NULL ORDER BY c.id ASC")
    List<Commit> findUnclassifiedByRepositoryId(@Param("repositoryId") Long repositoryId, Pageable pageable);

    Optional<Commit> findByIdAndRepositoryId(Long id, Long repositoryId);

    @Query(value = """
            SELECT c FROM Commit c
            WHERE c.repository.id = :repositoryId
              AND (:classification IS NULL OR c.classification = :classification)
              AND (:authorEmail IS NULL OR LOWER(c.authorEmail) = LOWER(:authorEmail))
              AND (CAST(:from AS instant) IS NULL OR c.committedAt >= :from)
              AND (CAST(:to AS instant) IS NULL OR c.committedAt <= :to)
            """,
            countQuery = """
            SELECT COUNT(c) FROM Commit c
            WHERE c.repository.id = :repositoryId
              AND (:classification IS NULL OR c.classification = :classification)
              AND (:authorEmail IS NULL OR LOWER(c.authorEmail) = LOWER(:authorEmail))
              AND (CAST(:from AS instant) IS NULL OR c.committedAt >= :from)
              AND (CAST(:to AS instant) IS NULL OR c.committedAt <= :to)
            """)
    Page<Commit> findByRepositoryIdWithFilters(
            @Param("repositoryId") Long repositoryId,
            @Param("classification") CommitClassification classification,
            @Param("authorEmail") String authorEmail,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );

    @Query("SELECT MAX(c.committedAt) FROM Commit c WHERE c.repository.id = :repositoryId")
    Instant findLatestCommittedAtByRepositoryId(@Param("repositoryId") Long repositoryId);
}
