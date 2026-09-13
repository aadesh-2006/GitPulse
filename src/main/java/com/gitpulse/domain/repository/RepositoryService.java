package com.gitpulse.domain.repository;

import com.gitpulse.common.exception.DuplicateResourceException;
import com.gitpulse.common.exception.ResourceNotFoundException;
import com.gitpulse.domain.repository.dto.CreateRepositoryRequest;
import com.gitpulse.domain.repository.dto.RepositoryResponse;
import com.gitpulse.integration.github.client.GitHubRepositoryClient;
import com.gitpulse.integration.github.dto.GitHubRepositoryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RepositoryService {

    private static final Logger log = LoggerFactory.getLogger(RepositoryService.class);

    private final RepositoryJpaRepository repositoryJpaRepository;
    private final GitHubRepositoryClient gitHubRepositoryClient;

    public RepositoryService(RepositoryJpaRepository repositoryJpaRepository,
                             GitHubRepositoryClient gitHubRepositoryClient) {
        this.repositoryJpaRepository = repositoryJpaRepository;
        this.gitHubRepositoryClient = gitHubRepositoryClient;
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

    @Transactional
    public RepositoryResponse syncRepositoryWithGitHub(Long id) {
        Repository repository = findEntityById(id);
        log.info("Starting GitHub metadata sync for repository [id={}, fullName={}]", id, repository.getFullName());

        GitHubRepositoryResponse githubData = gitHubRepositoryClient.getRepository(
                repository.getOwner(),
                repository.getName()
        );

        repository.updateFromGitHub(githubData);
        Repository updated = repositoryJpaRepository.save(repository);

        log.info("Completed GitHub metadata sync for repository [id={}, fullName={}]", id, updated.getFullName());
        return RepositoryResponse.fromEntity(updated);
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
