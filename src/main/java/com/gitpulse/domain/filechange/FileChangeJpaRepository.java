package com.gitpulse.domain.filechange;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Repository
public interface FileChangeJpaRepository extends JpaRepository<FileChange, Long> {

    @Query("SELECT DISTINCT f.commit.id FROM FileChange f WHERE f.commit.id IN :commitIds")
    Set<Long> findCommitIdsWithFileChanges(@Param("commitIds") Collection<Long> commitIds);

    @Query("SELECT f.filePath FROM FileChange f WHERE f.commit.id = :commitId AND f.filePath IN :filePaths")
    List<String> findExistingFilePaths(@Param("commitId") Long commitId, @Param("filePaths") Collection<String> filePaths);

    boolean existsByCommitIdAndFilePath(Long commitId, String filePath);

    long countByCommitId(Long commitId);

    List<FileChange> findByCommitId(Long commitId);

    List<FileChange> findByCommitIdOrderByFilePathAsc(Long commitId);
}
