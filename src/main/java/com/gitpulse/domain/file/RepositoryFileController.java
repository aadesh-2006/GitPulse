package com.gitpulse.domain.file;

import com.gitpulse.domain.file.dto.RepositoryFileResponse;
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
@RequestMapping("/api/v1/repositories/{repositoryId}/files")
public class RepositoryFileController {

    private final RepositoryFileQueryService repositoryFileQueryService;

    public RepositoryFileController(RepositoryFileQueryService repositoryFileQueryService) {
        this.repositoryFileQueryService = repositoryFileQueryService;
    }

    @GetMapping
    public ResponseEntity<Page<RepositoryFileResponse>> getRepositoryFiles(
            @PathVariable Long repositoryId,
            @RequestParam(required = false) String extension,
            @RequestParam(required = false) Boolean isDeleted,
            @PageableDefault(size = 20, sort = "totalChurn", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<RepositoryFileResponse> response = repositoryFileQueryService.getRepositoryFiles(
                repositoryId,
                extension,
                isDeleted,
                pageable
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/hotspots")
    public ResponseEntity<Page<RepositoryFileResponse>> getRepositoryHotspots(
            @PathVariable Long repositoryId,
            @RequestParam(required = false) String extension,
            @RequestParam(required = false) Boolean isDeleted,
            @PageableDefault(size = 20, sort = "totalChurn", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<RepositoryFileResponse> response = repositoryFileQueryService.getRepositoryHotspots(
                repositoryId,
                extension,
                isDeleted,
                pageable
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{*filePath}")
    public ResponseEntity<RepositoryFileResponse> getRepositoryFileByPath(
            @PathVariable Long repositoryId,
            @PathVariable String filePath
    ) {
        String cleanPath = filePath;
        if (cleanPath != null && cleanPath.startsWith("/")) {
            cleanPath = cleanPath.substring(1);
        }
        if (cleanPath == null || cleanPath.isBlank()) {
            throw new com.gitpulse.common.exception.AppException("filePath must not be blank");
        }
        RepositoryFileResponse response = repositoryFileQueryService.getRepositoryFileByPath(repositoryId, cleanPath);
        return ResponseEntity.ok(response);
    }
}