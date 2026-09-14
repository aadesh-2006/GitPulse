package com.gitpulse.domain.filechange;

import com.gitpulse.common.exception.ResourceNotFoundException;
import com.gitpulse.domain.commit.Commit;
import com.gitpulse.domain.commit.CommitJpaRepository;
import com.gitpulse.domain.filechange.dto.FileChangeIngestionResult;
import com.gitpulse.domain.repository.Repository;
import com.gitpulse.domain.repository.RepositoryJpaRepository;
import com.gitpulse.integration.github.client.GitHubCommitDetailsClient;
import com.gitpulse.integration.github.dto.GitHubCommitDetailResponse;
import com.gitpulse.integration.github.dto.GitHubFileResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class FileChangeIngestionService {

    private static final Logger log = LoggerFactory.getLogger(FileChangeIngestionService.class);
    private static final int COMMIT_BATCH_PAGE_SIZE = 50;

    private final RepositoryJpaRepository repositoryJpaRepository;
    private final CommitJpaRepository commitJpaRepository;
    private final FileChangeJpaRepository fileChangeJpaRepository;
    private final GitHubCommitDetailsClient gitHubCommitDetailsClient;

    public FileChangeIngestionService(RepositoryJpaRepository repositoryJpaRepository,
                                      CommitJpaRepository commitJpaRepository,
                                      FileChangeJpaRepository fileChangeJpaRepository,
                                      GitHubCommitDetailsClient gitHubCommitDetailsClient) {
        this.repositoryJpaRepository = repositoryJpaRepository;
        this.commitJpaRepository = commitJpaRepository;
        this.fileChangeJpaRepository = fileChangeJpaRepository;
        this.gitHubCommitDetailsClient = gitHubCommitDetailsClient;
    }

    public FileChangeIngestionResult ingestFileChanges(Long repositoryId, Long jobId) {
        Repository repository = repositoryJpaRepository.findById(repositoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository", "id", repositoryId));

        log.info("Starting file-change ingestion for repository [id={}, fullName={}, jobId={}]",
                repositoryId, repository.getFullName(), jobId);

        int commitsProcessed = 0;
        int commitsSkipped = 0;
        int filesReceived = 0;
        int filesInserted = 0;
        int duplicatesEncountered = 0;
        long startNanos = System.nanoTime();

        int pageNumber = 0;
        boolean hasMore = true;

        while (hasMore) {
            Page<Commit> commitPage = commitJpaRepository.findByRepositoryIdOrderByIdAsc(
                    repositoryId,
                    PageRequest.of(pageNumber, COMMIT_BATCH_PAGE_SIZE)
            );

            List<Commit> commits = commitPage.getContent();
            if (commits.isEmpty()) {
                break;
            }

            List<Long> commitIds = commits.stream().map(Commit::getId).toList();
            Set<Long> alreadyIngestedCommitIds = fileChangeJpaRepository.findCommitIdsWithFileChanges(commitIds);

            for (Commit commit : commits) {
                if (alreadyIngestedCommitIds.contains(commit.getId())) {
                    commitsSkipped++;
                    continue;
                }

                GitHubCommitDetailResponse detail = gitHubCommitDetailsClient.getCommitDetails(
                        repository.getOwner(),
                        repository.getName(),
                        commit.getGithubCommitSha()
                );

                commitsProcessed++;

                // Update commit stats if available and not yet set
                if (detail != null && detail.getStats() != null) {
                    boolean updated = false;
                    if (commit.getAdditions() == null && detail.getStats().getAdditions() != null) {
                        commit.setAdditions(detail.getStats().getAdditions());
                        updated = true;
                    }
                    if (commit.getDeletions() == null && detail.getStats().getDeletions() != null) {
                        commit.setDeletions(detail.getStats().getDeletions());
                        updated = true;
                    }
                    if (commit.getTotalChanges() == null && detail.getStats().getTotal() != null) {
                        commit.setTotalChanges(detail.getStats().getTotal());
                        updated = true;
                    }
                    if (updated) {
                        commitJpaRepository.save(commit);
                    }
                }

                List<GitHubFileResponse> files = (detail != null) ? detail.getFiles() : List.of();
                filesReceived += files.size();

                if (!files.isEmpty()) {
                    List<FileChange> fileChangesToPersist = new ArrayList<>();
                    Set<String> seenPathsInCommit = new HashSet<>();

                    for (GitHubFileResponse fileDto : files) {
                        String filePath = fileDto.getFilename();
                        if (filePath == null || filePath.isBlank()) {
                            continue;
                        }

                        if (!seenPathsInCommit.add(filePath)) {
                            duplicatesEncountered++;
                            continue;
                        }

                        FileChangeStatus status = FileChangeStatus.fromString(fileDto.getStatus());
                        FileChange fileChange = new FileChange(
                                commit,
                                filePath,
                                status,
                                fileDto.getAdditions(),
                                fileDto.getDeletions(),
                                fileDto.getChanges(),
                                fileDto.getBlobUrl(),
                                fileDto.getRawUrl()
                        );
                        fileChangesToPersist.add(fileChange);
                    }

                    if (!fileChangesToPersist.isEmpty()) {
                        fileChangeJpaRepository.saveAll(fileChangesToPersist);
                        filesInserted += fileChangesToPersist.size();
                    }
                }
            }

            hasMore = commitPage.hasNext();
            pageNumber++;
        }

        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
        FileChangeIngestionResult result = new FileChangeIngestionResult(
                commitsProcessed,
                commitsSkipped,
                filesReceived,
                filesInserted,
                duplicatesEncountered,
                durationMs
        );

        log.info("Completed file-change ingestion for repository [id={}, fullName={}, jobId={}]: {}",
                repositoryId, repository.getFullName(), jobId, result);

        return result;
    }
}
