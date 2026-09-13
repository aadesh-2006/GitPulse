package com.gitpulse.domain.repository;

import com.gitpulse.common.exception.DuplicateResourceException;
import com.gitpulse.common.exception.ResourceNotFoundException;
import com.gitpulse.domain.repository.dto.CreateRepositoryRequest;
import com.gitpulse.domain.repository.dto.RepositoryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RepositoryService {

    private static final Logger log = LoggerFactory.getLogger(RepositoryService.class);

    private final RepositoryJpaRepository repositoryJpaRepository;

    public RepositoryService(RepositoryJpaRepository repositoryJpaRepository) {
        this.repositoryJpaRepository = repositoryJpaRepository;
    }

    @Transactional
    public RepositoryResponse createRepository(CreateRepositoryRequest request) {
        String owner = request.getOwner().trim();
        String name = request.getName().trim();
        String fullName = owner + "/" + name;

        if (repositoryJpaRepository.existsByOwnerAndName(owner, name)) {
            log.warn("Attempted to register duplicate repository: {}", fullName);
            throw new DuplicateResourceException("Repository", "fullName", fullName);
        }

        Repository repository = new Repository(
                owner,
                name,
                request.getDescription(),
                request.getDefaultBranch()
        );

        Repository saved = repositoryJpaRepository.save(repository);
        log.info("Registered new repository [id={}, fullName={}]", saved.getId(), saved.getFullName());

        return RepositoryResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public RepositoryResponse getRepositoryById(Long id) {
        Repository repository = findEntityById(id);
        return RepositoryResponse.fromEntity(repository);
    }

    @Transactional(readOnly = true)
    public List<RepositoryResponse> getAllRepositories() {
        return repositoryJpaRepository.findAll()
                .stream()
                .map(RepositoryResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public Repository findEntityById(Long id) {
        return repositoryJpaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Repository", "id", id));
    }
}
