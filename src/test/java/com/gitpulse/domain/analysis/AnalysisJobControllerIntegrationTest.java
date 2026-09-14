package com.gitpulse.domain.analysis;

import com.gitpulse.domain.analysis.producer.AnalysisJobEventProducer;
import com.gitpulse.domain.repository.Repository;
import com.gitpulse.domain.repository.RepositoryJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AnalysisJobControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RepositoryJpaRepository repositoryJpaRepository;

    @Autowired
    private AnalysisJobJpaRepository analysisJobJpaRepository;

    @MockBean
    private AnalysisJobEventProducer analysisJobEventProducer;

    @Test
    @DisplayName("POST /api/v1/repositories/{repositoryId}/analysis-jobs - Should return 201 Created with PENDING status")
    void createAnalysisJob_ValidRepository_Returns201() throws Exception {
        Repository repo = repositoryJpaRepository.save(new Repository("openjdk", "jdk", "Java Development Kit", "master"));

        mockMvc.perform(post("/api/v1/repositories/{repositoryId}/analysis-jobs", repo.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.repositoryId", is(repo.getId().intValue())))
                .andExpect(jsonPath("$.repositoryFullName", is("openjdk/jdk")))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/repositories/{repositoryId}/analysis-jobs - Should return 404 Not Found for missing repository")
    void createAnalysisJob_MissingRepository_Returns404() throws Exception {
        mockMvc.perform(post("/api/v1/repositories/{repositoryId}/analysis-jobs", 888888L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is("Repository not found with id: '888888'")));
    }

    @Test
    @DisplayName("GET /api/v1/analysis-jobs/{jobId} - Should return 200 OK for existing job")
    void getAnalysisJobById_Found_Returns200() throws Exception {
        Repository repo = repositoryJpaRepository.save(new Repository("rust-lang", "rust", "Empowering everyone to build reliable and efficient software", "master"));
        AnalysisJob job = analysisJobJpaRepository.save(new AnalysisJob(repo, AnalysisJobStatus.PENDING));

        mockMvc.perform(get("/api/v1/analysis-jobs/{jobId}", job.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(job.getId().intValue())))
                .andExpect(jsonPath("$.repositoryId", is(repo.getId().intValue())))
                .andExpect(jsonPath("$.status", is("PENDING")));
    }

    @Test
    @DisplayName("GET /api/v1/analysis-jobs/{jobId} - Should return 404 Not Found for missing job")
    void getAnalysisJobById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/v1/analysis-jobs/{jobId}", 777777L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is("AnalysisJob not found with id: '777777'")));
    }

    @Test
    @DisplayName("GET /api/v1/repositories/{repositoryId}/analysis-jobs - Should return job history for repository")
    void getJobsByRepositoryId_Returns200() throws Exception {
        Repository repo = repositoryJpaRepository.save(new Repository("vuejs", "core", "Vue.js core", "main"));
        analysisJobJpaRepository.save(new AnalysisJob(repo, AnalysisJobStatus.PENDING));

        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/analysis-jobs", repo.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].repositoryFullName", is("vuejs/core")))
                .andExpect(jsonPath("$[0].status", is("PENDING")));
    }
}
