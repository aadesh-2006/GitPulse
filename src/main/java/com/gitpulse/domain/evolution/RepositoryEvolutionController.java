package com.gitpulse.domain.evolution;

import com.gitpulse.domain.evolution.dto.RepositoryEvolutionComparisonResponse;
import com.gitpulse.domain.evolution.dto.RepositoryEvolutionCompositionResponse;
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
    private final RepositoryEvolutionComparisonQueryService comparisonQueryService;
    private final RepositoryEvolutionCompositionQueryService compositionQueryService;

    public RepositoryEvolutionController(
            RepositoryEvolutionQueryService evolutionQueryService,
            RepositoryEvolutionComparisonQueryService comparisonQueryService,
            RepositoryEvolutionCompositionQueryService compositionQueryService) {
        this.evolutionQueryService = evolutionQueryService;
        this.comparisonQueryService = comparisonQueryService;
        this.compositionQueryService = compositionQueryService;
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

    @GetMapping("/compare")
    public ResponseEntity<RepositoryEvolutionComparisonResponse> compareEvolution(
            @PathVariable Long repositoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant currentFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant currentTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant previousFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant previousTo
    ) {
        RepositoryEvolutionComparisonResponse response = comparisonQueryService.compareRepositoryEvolution(
                repositoryId, currentFrom, currentTo, previousFrom, previousTo
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/composition")
    public ResponseEntity<RepositoryEvolutionCompositionResponse> getEvolutionComposition(
            @PathVariable Long repositoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        RepositoryEvolutionCompositionResponse response = compositionQueryService.getEvolutionComposition(
                repositoryId, from, to
        );
        return ResponseEntity.ok(response);
    }
}
