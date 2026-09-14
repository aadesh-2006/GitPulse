package com.gitpulse.domain.analysis;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnalysisJobJpaRepository extends JpaRepository<AnalysisJob, Long> {

    List<AnalysisJob> findByRepositoryIdOrderByCreatedAtDesc(Long repositoryId);

    @EntityGraph(attributePaths = {"repository"})
    @Query("SELECT j FROM AnalysisJob j WHERE j.id = :id")
    Optional<AnalysisJob> findWithRepositoryById(@Param("id") Long id);
}
