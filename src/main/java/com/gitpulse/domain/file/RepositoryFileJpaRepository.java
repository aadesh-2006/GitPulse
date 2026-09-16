package com.gitpulse.domain.file;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepositoryFileJpaRepository extends JpaRepository<RepositoryFile, Long> {

    Optional<RepositoryFile> findByRepositoryIdAndFilePath(Long repositoryId, String filePath);

    List<RepositoryFile> findByRepositoryId(Long repositoryId);

    Page<RepositoryFile> findByRepositoryId(Long repositoryId, Pageable pageable);
}