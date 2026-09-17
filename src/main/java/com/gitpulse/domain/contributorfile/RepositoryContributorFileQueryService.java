package com.gitpulse.domain.contributorfile;

import com.gitpulse.common.exception.AppException;
import com.gitpulse.common.exception.ResourceNotFoundException;
import com.gitpulse.domain.contributor.Contributor;
import com.gitpulse.domain.contributor.ContributorJpaRepository;
import com.gitpulse.domain.contributorfile.dto.RepositoryContributorFileResponse;
import com.gitpulse.domain.repository.RepositoryJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class RepositoryContributorFileQueryService {

    private final RepositoryJpaRepository repositoryJpaRepository;
    private final ContributorJpaRepository contributorJpaRepository;
    private final RepositoryContributorFileJpaRepository repositoryContributorFileJpaRepository;

    public RepositoryContributorFileQueryService(RepositoryJpaRepository repositoryJpaRepository,
                                                ContributorJpaRepository contributorJpaRepository,
                                                RepositoryContributorFileJpaRepository repositoryContributorFileJpaRepository) {
        this.repositoryJpaRepository = Objects.requireNonNull(repositoryJpaRepository, "repositoryJpaRepository must not be null");
        this.contributorJpaRepository = Objects.requireNonNull(contributorJpaRepository, "contributorJpaRepository must not be null");
        this.repositoryContributorFileJpaRepository = Objects.requireNonNull(repositoryContributorFileJpaRepository, "repositoryContributorFileJpaRepository must not be null");
    }

    public Page<RepositoryContributorFileResponse> getRepositoryContributorFiles(Long repositoryId, Pageable pageable) {
        if (!repositoryJpaRepository.existsById(repositoryId)) {
            throw new ResourceNotFoundException("Repository not found with id: " + repositoryId);
        }

        Pageable sanitizedPageable = RepositoryContributorFileSortValidator.validateAndSanitize(pageable);
        return repositoryContributorFileJpaRepository
                .findByRepositoryId(repositoryId, sanitizedPageable)
                .map(RepositoryContributorFileResponse::fromEntity);
    }

    public Page<RepositoryContributorFileResponse> getContributorFilesByContributor(Long repositoryId, Long contributorId, Pageable pageable) {
        if (!repositoryJpaRepository.existsById(repositoryId)) {
            throw new ResourceNotFoundException("Repository not found with id: " + repositoryId);
        }
        if (!contributorJpaRepository.existsById(contributorId)) {
            throw new ResourceNotFoundException("Contributor not found with id: " + contributorId);
        }

        Pageable sanitizedPageable = RepositoryContributorFileSortValidator.validateAndSanitize(pageable);
        return repositoryContributorFileJpaRepository
                .findByRepositoryIdAndContributorId(repositoryId, contributorId, sanitizedPageable)
                .map(RepositoryContributorFileResponse::fromEntity);
    }

    public Page<RepositoryContributorFileResponse> getContributorFilesByFilePath(Long repositoryId, String filePath, Pageable pageable) {
        if (filePath == null || filePath.isBlank()) {
            throw new AppException("filePath query parameter must not be blank");
        }
        if (!repositoryJpaRepository.existsById(repositoryId)) {
            throw new ResourceNotFoundException("Repository not found with id: " + repositoryId);
        }

        Pageable sanitizedPageable = RepositoryContributorFileSortValidator.validateAndSanitize(pageable);
        return repositoryContributorFileJpaRepository
                .findByRepositoryIdAndFilePath(repositoryId, filePath.trim(), sanitizedPageable)
                .map(RepositoryContributorFileResponse::fromEntity);
    }

    public RepositoryContributorFileResponse getContributorFile(Long repositoryId, Long contributorId, String filePath) {
        if (filePath == null || filePath.isBlank()) {
            throw new AppException("filePath must not be blank");
        }

        String cleanPath = filePath.trim();
        if (cleanPath.startsWith("/")) {
            cleanPath = cleanPath.substring(1);
        }

        final String targetPath = cleanPath;
        return repositoryContributorFileJpaRepository
                .findByRepositoryIdAndContributorIdAndFilePath(repositoryId, contributorId, targetPath)
                .map(RepositoryContributorFileResponse::fromEntity)
                .orElseGet(() -> {
                    if (!repositoryJpaRepository.existsById(repositoryId)) {
                        throw new ResourceNotFoundException("Repository not found with id: " + repositoryId);
                    }
                    if (!contributorJpaRepository.existsById(contributorId)) {
                        throw new ResourceNotFoundException("Contributor not found with id: " + contributorId);
                    }
                    throw new ResourceNotFoundException("Contributor-file relationship not found for repository id: " + repositoryId + ", contributor id: " + contributorId + ", filePath: '" + targetPath + "'");
                });
    }

    public Page<com.gitpulse.domain.contributorfile.dto.RepositoryFileOwnershipResponse> getRepositoryFileOwnership(
            Long repositoryId,
            Pageable pageable
    ) {
        if (!repositoryJpaRepository.existsById(repositoryId)) {
            throw new ResourceNotFoundException("Repository not found with id: " + repositoryId);
        }

        Pageable sanitizedPageable = RepositoryFileOwnershipSortValidator.validateAndSanitize(pageable);
        Page<com.gitpulse.domain.contributorfile.dto.RepositoryFileOwnershipRow> rowsPage =
                repositoryContributorFileJpaRepository.findFileOwnershipByRepositoryId(repositoryId, sanitizedPageable);

        java.util.Set<Long> topContributorIds = rowsPage.getContent().stream()
                .map(com.gitpulse.domain.contributorfile.dto.RepositoryFileOwnershipRow::getTopContributorId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        java.util.Map<Long, Contributor> contributorMap = topContributorIds.isEmpty()
                ? java.util.Collections.emptyMap()
                : contributorJpaRepository.findAllById(topContributorIds).stream()
                .collect(java.util.stream.Collectors.toMap(Contributor::getId, java.util.function.Function.identity()));

        return rowsPage.map(row -> new com.gitpulse.domain.contributorfile.dto.RepositoryFileOwnershipResponse(
                row.getRepositoryId(),
                row.getFilePath(),
                row.getContributorCount(),
                row.getTotalRevisionsAcrossContributors(),
                row.getTopContributorId() != null
                        ? com.gitpulse.domain.contributorfile.dto.RepositoryFileOwnershipResponse.ContributorSummary.fromEntity(contributorMap.get(row.getTopContributorId()))
                        : null,
                row.getTopContributorRevisionShare() != null ? row.getTopContributorRevisionShare() : 0.0
        ));
    }
}
