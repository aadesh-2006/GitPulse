package com.gitpulse.domain.contributor;

import com.gitpulse.domain.contributor.dto.ContributorResponse;
import com.gitpulse.domain.contributor.dto.RepositoryContributorResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ContributorController {

    private final ContributorService contributorService;

    public ContributorController(ContributorService contributorService) {
        this.contributorService = contributorService;
    }

    @GetMapping("/repositories/{repositoryId}/contributors")
    public ResponseEntity<Page<RepositoryContributorResponse>> getRepositoryContributors(
            @PathVariable Long repositoryId,
            @PageableDefault(size = 20, sort = "totalCommits", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<RepositoryContributorResponse> page = contributorService.getRepositoryContributors(repositoryId, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/contributors/{contributorId}")
    public ResponseEntity<ContributorResponse> getContributorById(@PathVariable Long contributorId) {
        ContributorResponse response = contributorService.getContributorById(contributorId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/repositories/{repositoryId}/contributors/{contributorId}")
    public ResponseEntity<RepositoryContributorResponse> getRepositoryContributor(
            @PathVariable Long repositoryId,
            @PathVariable Long contributorId
    ) {
        RepositoryContributorResponse response = contributorService.getRepositoryContributor(repositoryId, contributorId);
        return ResponseEntity.ok(response);
    }
}
