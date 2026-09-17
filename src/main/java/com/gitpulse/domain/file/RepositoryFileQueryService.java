package com.gitpulse.domain.file;

import com.gitpulse.common.exception.ResourceNotFoundException;
import com.gitpulse.domain.file.dto.RepositoryFileResponse;
import com.gitpulse.domain.repository.RepositoryJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class RepositoryFileQueryService {

    private final RepositoryJpaRepository repositoryJpaRepository;
    private final RepositoryFileJpaRepository repositoryFileJpaRepository;

    public RepositoryFileQueryService(RepositoryJpaRepository repositoryJpaRepository,
                                      RepositoryFileJpaRepository repositoryFileJpaRepository) {
        this.repositoryJpaRepository = Objects.requireNonNull(repositoryJpaRepository, "repositoryJpaRepository must not be null");
        this.repositoryFileJpaRepository = Objects.requireNonNull(repositoryFileJpaRepository, "repositoryFileJpaRepository must not be null");
    }

    public Page<RepositoryFileResponse> getRepositoryFiles(Long repositoryId,
                                                          String extension,
                                                          Boolean isDeleted,
                                                          Pageable pageable) {
        if (!repositoryJpaRepository.existsById(repositoryId)) {
            throw new ResourceNotFoundException("Repository not found with id: " + repositoryId);
        }

        Pageable sanitizedPageable = RepositoryFileSortValidator.validateAndSanitize(pageable);
        return repositoryFileJpaRepository
                .findByRepositoryIdWithFilters(repositoryId, extension, isDeleted, sanitizedPageable)
                .map(RepositoryFileResponse::fromEntity);
    }

    public RepositoryFileResponse getRepositoryFileByPath(Long repositoryId, String filePath) {
        return repositoryFileJpaRepository.findByRepositoryIdAndFilePath(repositoryId, filePath)
                .map(RepositoryFileResponse::fromEntity)
                .orElseGet(() -> {
                    if (!repositoryJpaRepository.existsById(repositoryId)) {
                        throw new ResourceNotFoundException("Repository not found with id: " + repositoryId);
                    }
                    throw new ResourceNotFoundException("File not found with path '" + filePath + "' in repository id: " + repositoryId);
                });
    }

    public Page<RepositoryFileResponse> getRepositoryHotspots(Long repositoryId,
                                                             String extension,
                                                             Boolean isDeleted,
                                                             Pageable pageable) {
        return getRepositoryFiles(repositoryId, extension, isDeleted, pageable);
    }
}