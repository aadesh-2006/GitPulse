package com.gitpulse.domain.commit;

import com.gitpulse.common.exception.ResourceNotFoundException;
import com.gitpulse.domain.commit.dto.CommitIngestionResult;
import com.gitpulse.domain.repository.Repository;
import com.gitpulse.domain.repository.RepositoryJpaRepository;
import com.gitpulse.integration.github.client.GitHubCommitClient;
import com.gitpulse.integration.github.config.GitHubProperties;
import com.gitpulse.integration.github.dto.GitHubCommitPageResponse;
import com.gitpulse.integration.github.dto.GitHubCommitResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class CommitIngestionService {

    private static final Logger log = LoggerFactory.getLogger(CommitIngestionService.class);

    private final RepositoryJpaRepository repositoryJpaRepository;
    private final CommitJpaRepository commitJpaRepository;
    private final GitHubCommitClient gitHubCommitClient;
    private final GitHubProperties gitHubProperties;

    public CommitIngestionService(RepositoryJpaRepository repositoryJpaRepository,
                                  CommitJpaRepository commitJpaRepository,
                                  GitHubCommitClient gitHubCommitClient,
                                  GitHubProperties gitHubProperties) {
        this.repositoryJpaRepository = repositoryJpaRepository;
        this.commitJpaRepository = commitJpaRepository;
        this.gitHubCommitClient = gitHubCommitClient;
        this.gitHubProperties = gitHubProperties;
    }

    public CommitIngestionResult ingestCommits(Long repositoryId, Long jobId) {
        Repository repository = repositoryJpaRepository.findById(repositoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository", "id", repositoryId));

        int pageSize = gitHubProperties.getCommitPageSize();
        log.info("Starting commit ingestion for repository [id={}, fullName={}] with page size {}",
                repositoryId, repository.getFullName(), pageSize);

        int pagesProcessed = 0;
        int commitsReceived = 0;
        int commitsInserted = 0;
        int duplicatesEncountered = 0;
        long startNanos = System.nanoTime();

        int page = 1;
        boolean hasMore = true;

        while (hasMore) {
            GitHubCommitPageResponse pageResponse = gitHubCommitClient.getCommitsPage(
                    repository.getOwner(),
                    repository.getName(),
                    page,
                    pageSize
            );

            List<GitHubCommitResponse> commits = pageResponse.getCommits();
            if (commits == null || commits.isEmpty()) {
                log.debug("Received empty commits page for repository [id={}, page={}]", repositoryId, page);
                break;
            }

            pagesProcessed++;
            commitsReceived += commits.size();

            List<String> pageShas = commits.stream()
                    .map(GitHubCommitResponse::getSha)
                    .filter(sha -> sha != null && !sha.isBlank())
                    .toList();

            Set<String> existingShas = new HashSet<>(
                    commitJpaRepository.findExistingGithubCommitShas(repositoryId, pageShas)
            );

            List<Commit> newEntities = new ArrayList<>();
            int pageDuplicates = 0;

            for (GitHubCommitResponse dto : commits) {
                if (dto.getSha() != null && !existingShas.contains(dto.getSha())) {
                    newEntities.add(mapToEntity(repository, dto));
                } else {
                    pageDuplicates++;
                }
            }

            if (!newEntities.isEmpty()) {
                saveBatch(newEntities);
                commitsInserted += newEntities.size();
            }

            duplicatesEncountered += pageDuplicates;

            log.info("[jobId={}, repo={}] Page {}: received {}, inserted {}, duplicates {}",
                    jobId, repository.getFullName(), page, commits.size(), newEntities.size(), pageDuplicates);

            hasMore = pageResponse.hasNextPage();
            page++;
        }

        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
        CommitIngestionResult result = new CommitIngestionResult(
                pagesProcessed,
                commitsReceived,
                commitsInserted,
                duplicatesEncountered,
                durationMs
        );

        log.info("Completed commit ingestion for repository [id={}, fullName={}, jobId={}]: {}",
                repositoryId, repository.getFullName(), jobId, result);

        return result;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveBatch(List<Commit> commits) {
        commitJpaRepository.saveAll(commits);
    }

    private Commit mapToEntity(Repository repository, GitHubCommitResponse dto) {
        String message = "";
        String authorName = null;
        String authorEmail = null;
        Instant committedAt = null;

        if (dto.getCommit() != null) {
            message = dto.getCommit().getMessage() != null ? dto.getCommit().getMessage() : "";
            if (dto.getCommit().getAuthor() != null) {
                authorName = dto.getCommit().getAuthor().getName();
                authorEmail = dto.getCommit().getAuthor().getEmail();
                committedAt = dto.getCommit().getAuthor().getDate();
            }
            if (committedAt == null && dto.getCommit().getCommitter() != null) {
                committedAt = dto.getCommit().getCommitter().getDate();
            }
        }

        if (committedAt == null) {
            committedAt = Instant.now();
        }

        String authorUsername = dto.getAuthor() != null ? dto.getAuthor().getLogin() : null;

        Integer additions = null;
        Integer deletions = null;
        Integer totalChanges = null;

        if (dto.getStats() != null) {
            additions = dto.getStats().getAdditions();
            deletions = dto.getStats().getDeletions();
            totalChanges = dto.getStats().getTotal();
        }

        return new Commit(
                repository,
                dto.getSha(),
                message,
                authorName,
                authorEmail,
                authorUsername,
                committedAt,
                additions,
                deletions,
                totalChanges,
                dto.getHtmlUrl()
        );
    }
}
