package com.gitpulse.domain.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisJobJpaRepository extends JpaRepository<AnalysisJob, Long> {

    List<AnalysisJob> findByRepositoryIdOrderByCreatedAtDesc(Long repositoryId);
}
