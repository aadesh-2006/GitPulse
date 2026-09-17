package com.gitpulse.domain.commit;

import com.gitpulse.domain.commit.dto.CommitDetailResponse;
import com.gitpulse.domain.commit.dto.CommitResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/repositories/{repositoryId}/commits")
public class CommitController {

    private final CommitQueryService commitQueryService;

    public CommitController(CommitQueryService commitQueryService) {
        this.commitQueryService = commitQueryService;
    }

    @GetMapping
    public ResponseEntity<Page<CommitResponse>> getRepositoryCommits(
            @PathVariable Long repositoryId,
            @RequestParam(required = false) CommitClassification classification,
            @RequestParam(required = false) String authorEmail,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20, sort = "committedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<CommitResponse> response = commitQueryService.getRepositoryCommits(
                repositoryId,
                classification,
                authorEmail,
                from,
                to,
                pageable
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{commitId}")
    public ResponseEntity<CommitDetailResponse> getCommitDetail(
            @PathVariable Long repositoryId,
            @PathVariable Long commitId
    ) {
        CommitDetailResponse response = commitQueryService.getCommitDetail(repositoryId, commitId);
        return ResponseEntity.ok(response);
    }
}
