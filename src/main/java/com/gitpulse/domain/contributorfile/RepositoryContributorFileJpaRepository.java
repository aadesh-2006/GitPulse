package com.gitpulse.domain.contributorfile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepositoryContributorFileJpaRepository extends JpaRepository<RepositoryContributorFile, Long> {

    List<RepositoryContributorFile> findByRepositoryId(Long repositoryId);

    Optional<RepositoryContributorFile> findByRepositoryIdAndContributorIdAndFilePath(Long repositoryId, Long contributorId, String filePath);

    List<RepositoryContributorFile> findByRepositoryIdAndContributorId(Long repositoryId, Long contributorId);

    List<RepositoryContributorFile> findByRepositoryIdAndFilePath(Long repositoryId, String filePath);

    void deleteByRepositoryId(Long repositoryId);

    long countByRepositoryId(Long repositoryId);
}
