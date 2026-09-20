package com.gitpulse.domain.contributor;

import com.gitpulse.common.exception.ResourceNotFoundException;
import com.gitpulse.domain.contributor.dto.ContributorResponse;
import com.gitpulse.domain.contributor.dto.RepositoryContributorResponse;
import com.gitpulse.domain.repository.RepositoryJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ContributorService {

    private final ContributorJpaRepository contributorJpaRepository;
    private final RepositoryContributorJpaRepository repositoryContributorJpaRepository;
    private final RepositoryJpaRepository repositoryJpaRepository;

    public ContributorService(ContributorJpaRepository contributorJpaRepository,
                              RepositoryContributorJpaRepository repositoryContributorJpaRepository,
                              RepositoryJpaRepository repositoryJpaRepository) {
        this.contributorJpaRepository = contributorJpaRepository;
        this.repositoryContributorJpaRepository = repositoryContributorJpaRepository;
        this.repositoryJpaRepository = repositoryJpaRepository;
    }

    public Page<RepositoryContributorResponse> getRepositoryContributors(Long repositoryId, Pageable pageable) {
        if (!repositoryJpaRepository.existsById(repositoryId)) {
            throw new ResourceNotFoundException("Repository", "id", repositoryId);
        }

        Pageable sanitizedPageable = ContributorSortValidator.validateAndSanitize(pageable);
        return repositoryContributorJpaRepository.findByRepositoryId(repositoryId, sanitizedPageable)
                .map(RepositoryContributorResponse::fromEntity);
    }

    public ContributorResponse getContributorById(Long contributorId) {
        Contributor contributor = contributorJpaRepository.findById(contributorId)
                .orElseThrow(() -> new ResourceNotFoundException("Contributor", "id", contributorId));
        return ContributorResponse.fromEntity(contributor);
    }

    public RepositoryContributorResponse getRepositoryContributor(Long repositoryId, Long contributorId) {
        if (!repositoryJpaRepository.existsById(repositoryId)) {
            throw new ResourceNotFoundException("Repository", "id", repositoryId);
        }

        RepositoryContributor rc = repositoryContributorJpaRepository.findByRepositoryIdAndContributorId(repositoryId, contributorId)
                .orElseThrow(() -> new ResourceNotFoundException("RepositoryContributor", "repositoryId=" + repositoryId + ", contributorId", contributorId));

        return RepositoryContributorResponse.fromEntity(rc);
    }
}
