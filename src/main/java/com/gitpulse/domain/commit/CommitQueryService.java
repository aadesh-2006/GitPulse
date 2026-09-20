package com.gitpulse.domain.commit;

import com.gitpulse.common.exception.ResourceNotFoundException;
import com.gitpulse.domain.commit.dto.CommitDetailResponse;
import com.gitpulse.domain.commit.dto.CommitResponse;
import com.gitpulse.domain.filechange.FileChange;
import com.gitpulse.domain.filechange.FileChangeJpaRepository;
import com.gitpulse.domain.repository.RepositoryJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class CommitQueryService {

    private final RepositoryJpaRepository repositoryJpaRepository;
    private final CommitJpaRepository commitJpaRepository;
    private final FileChangeJpaRepository fileChangeJpaRepository;

    public CommitQueryService(RepositoryJpaRepository repositoryJpaRepository,
                              CommitJpaRepository commitJpaRepository,
                              FileChangeJpaRepository fileChangeJpaRepository) {
        this.repositoryJpaRepository = Objects.requireNonNull(repositoryJpaRepository, "repositoryJpaRepository must not be null");
        this.commitJpaRepository = Objects.requireNonNull(commitJpaRepository, "commitJpaRepository must not be null");
        this.fileChangeJpaRepository = Objects.requireNonNull(fileChangeJpaRepository, "fileChangeJpaRepository must not be null");
    }

    public Page<CommitResponse> getRepositoryCommits(
            Long repositoryId,
            CommitClassification classification,
            String authorEmail,
            Instant from,
            Instant to,
            Pageable pageable
    ) {
        if (from != null && to != null && !from.isBefore(to)) {
            throw new IllegalArgumentException("The 'from' timestamp must be strictly before the 'to' timestamp");
        }

        if (!repositoryJpaRepository.existsById(repositoryId)) {
            throw new ResourceNotFoundException("Repository not found with id: " + repositoryId);
        }

        Pageable sanitizedPageable = CommitSortValidator.validateAndSanitize(pageable);
        String normalizedEmail = (authorEmail != null && !authorEmail.isBlank()) ? authorEmail.trim() : null;

        return commitJpaRepository
                .findByRepositoryIdWithFilters(repositoryId, classification, normalizedEmail, from, to, sanitizedPageable)
                .map(CommitResponse::fromEntity);
    }

    public CommitDetailResponse getCommitDetail(Long repositoryId, Long commitId) {
        Commit commit = commitJpaRepository.findByIdAndRepositoryId(commitId, repositoryId)
                .orElseGet(() -> {
                    if (!repositoryJpaRepository.existsById(repositoryId)) {
                        throw new ResourceNotFoundException("Repository not found with id: " + repositoryId);
                    }
                    throw new ResourceNotFoundException("Commit not found with id: " + commitId + " for repository id: " + repositoryId);
                });

        List<FileChange> fileChanges = fileChangeJpaRepository.findByCommitIdOrderByFilePathAsc(commitId);
        return CommitDetailResponse.fromEntity(commit, fileChanges);
    }
}
