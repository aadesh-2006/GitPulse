package com.gitpulse.domain.analysis;

import com.gitpulse.domain.analysis.dto.AnalysisJobResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class AnalysisJobController {

    private final AnalysisJobService analysisJobService;

    public AnalysisJobController(AnalysisJobService analysisJobService) {
        this.analysisJobService = analysisJobService;
    }

    @PostMapping("/repositories/{repositoryId}/analysis-jobs")
    public ResponseEntity<AnalysisJobResponse> createAnalysisJob(@PathVariable Long repositoryId) {
        AnalysisJobResponse response = analysisJobService.createAnalysisJob(repositoryId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/analysis-jobs/{jobId}")
    public ResponseEntity<AnalysisJobResponse> getAnalysisJobById(@PathVariable Long jobId) {
        AnalysisJobResponse response = analysisJobService.getAnalysisJobById(jobId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/repositories/{repositoryId}/analysis-jobs")
    public ResponseEntity<List<AnalysisJobResponse>> getJobsByRepositoryId(@PathVariable Long repositoryId) {
        List<AnalysisJobResponse> responses = analysisJobService.getJobsByRepositoryId(repositoryId);
        return ResponseEntity.ok(responses);
    }
}
