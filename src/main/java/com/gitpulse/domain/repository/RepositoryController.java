package com.gitpulse.domain.repository;

import com.gitpulse.domain.repository.dto.CreateRepositoryRequest;
import com.gitpulse.domain.repository.dto.RepositoryResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/repositories")
public class RepositoryController {

    private final RepositoryService repositoryService;

    public RepositoryController(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @PostMapping
    public ResponseEntity<RepositoryResponse> createRepository(@Valid @RequestBody CreateRepositoryRequest request) {
        RepositoryResponse response = repositoryService.createRepository(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/sync")
    public ResponseEntity<RepositoryResponse> syncRepository(@PathVariable Long id) {
        RepositoryResponse response = repositoryService.syncRepositoryWithGitHub(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RepositoryResponse> getRepositoryById(@PathVariable Long id) {
        RepositoryResponse response = repositoryService.getRepositoryById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<RepositoryResponse>> getAllRepositories() {
        List<RepositoryResponse> responses = repositoryService.getAllRepositories();
        return ResponseEntity.ok(responses);
    }
}
