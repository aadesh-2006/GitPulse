package com.gitpulse.domain.contributorfile;

import com.gitpulse.domain.contributorfile.dto.RepositoryContributorFileResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/repositories/{repositoryId}")
public class RepositoryContributorFileController {

    private final RepositoryContributorFileQueryService queryService;

    public RepositoryContributorFileController(RepositoryContributorFileQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/contributor-files")
    public ResponseEntity<Page<RepositoryContributorFileResponse>> getContributorFiles(
            @PathVariable Long repositoryId,
            @PageableDefault(size = 20, sort = "totalChurn", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<RepositoryContributorFileResponse> response = queryService.getRepositoryContributorFiles(repositoryId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/contributors/{contributorId}/files")
    public ResponseEntity<Page<RepositoryContributorFileResponse>> getContributorFilesByContributor(
            @PathVariable Long repositoryId,
            @PathVariable Long contributorId,
            @PageableDefault(size = 20, sort = "totalChurn", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<RepositoryContributorFileResponse> response = queryService.getContributorFilesByContributor(repositoryId, contributorId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/files/contributors")
    public ResponseEntity<Page<RepositoryContributorFileResponse>> getContributorFilesByFilePath(
            @PathVariable Long repositoryId,
            @RequestParam(required = false) String filePath,
            @PageableDefault(size = 20, sort = "totalChurn", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<RepositoryContributorFileResponse> response = queryService.getContributorFilesByFilePath(repositoryId, filePath, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/contributors/{contributorId}/files/{*filePath}")
    public ResponseEntity<RepositoryContributorFileResponse> getContributorFile(
            @PathVariable Long repositoryId,
            @PathVariable Long contributorId,
            @PathVariable String filePath
    ) {
        RepositoryContributorFileResponse response = queryService.getContributorFile(repositoryId, contributorId, filePath);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/file-ownership")
    public ResponseEntity<Page<com.gitpulse.domain.contributorfile.dto.RepositoryFileOwnershipResponse>> getFileOwnership(
            @PathVariable Long repositoryId,
            @PageableDefault(size = 20, sort = "topContributorRevisionShare", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<com.gitpulse.domain.contributorfile.dto.RepositoryFileOwnershipResponse> response =
                queryService.getRepositoryFileOwnership(repositoryId, pageable);
        return ResponseEntity.ok(response);
    }
}
