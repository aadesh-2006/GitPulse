package com.gitpulse.domain.evolution;

import com.gitpulse.domain.evolution.dto.RepositoryEvolutionResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/repositories/{repositoryId}/evolution")
public class RepositoryEvolutionController {

    private final RepositoryEvolutionQueryService evolutionQueryService;

    public RepositoryEvolutionController(RepositoryEvolutionQueryService evolutionQueryService) {
        this.evolutionQueryService = evolutionQueryService;
    }

    @GetMapping
    public ResponseEntity<RepositoryEvolutionResponse> getRepositoryEvolution(
            @PathVariable Long repositoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        RepositoryEvolutionResponse response = evolutionQueryService.getRepositoryEvolution(repositoryId, from, to);
        return ResponseEntity.ok(response);
    }
}
