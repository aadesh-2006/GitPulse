package com.gitpulse.domain.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitpulse.domain.repository.dto.CreateRepositoryRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
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
class RepositoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RepositoryJpaRepository repositoryJpaRepository;

    @Test
    @DisplayName("POST /api/v1/repositories - Should return 201 Created for valid repository")
    void createRepository_Valid_Returns201() throws Exception {
        CreateRepositoryRequest request = new CreateRepositoryRequest("torvalds", "linux", "Linux kernel source tree", "master");

        mockMvc.perform(post("/api/v1/repositories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.owner", is("torvalds")))
                .andExpect(jsonPath("$.name", is("linux")))
                .andExpect(jsonPath("$.fullName", is("torvalds/linux")))
                .andExpect(jsonPath("$.defaultBranch", is("master")))
                .andExpect(jsonPath("$.description", is("Linux kernel source tree")))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/repositories - Should return 400 Bad Request when owner or name is blank")
    void createRepository_BlankFields_Returns400() throws Exception {
        CreateRepositoryRequest request = new CreateRepositoryRequest("", "  ");

        mockMvc.perform(post("/api/v1/repositories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.validationErrors.owner", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.name", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/repositories - Should return 409 Conflict when repository already registered")
    void createRepository_Duplicate_Returns409() throws Exception {
        Repository existing = new Repository("facebook", "react", "A JavaScript library for building user interfaces", "main");
        repositoryJpaRepository.save(existing);

        CreateRepositoryRequest request = new CreateRepositoryRequest("facebook", "react");

        mockMvc.perform(post("/api/v1/repositories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", is("Repository already exists with fullName: 'facebook/react'")));
    }

    @Test
    @DisplayName("GET /api/v1/repositories/{id} - Should return 200 OK for existing repository")
    void getRepositoryById_Found_Returns200() throws Exception {
        Repository saved = repositoryJpaRepository.save(new Repository("golang", "go", "The Go programming language", "master"));

        mockMvc.perform(get("/api/v1/repositories/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(saved.getId().intValue())))
                .andExpect(jsonPath("$.owner", is("golang")))
                .andExpect(jsonPath("$.name", is("go")))
                .andExpect(jsonPath("$.fullName", is("golang/go")));
    }

    @Test
    @DisplayName("GET /api/v1/repositories/{id} - Should return 404 Not Found for nonexistent repository")
    void getRepositoryById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/v1/repositories/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is("Repository not found with id: '999999'")));
    }

    @Test
    @DisplayName("GET /api/v1/repositories - Should return 200 OK with repository list")
    void getAllRepositories_Returns200() throws Exception {
        repositoryJpaRepository.save(new Repository("apache", "kafka", "Apache Kafka", "trunk"));

        mockMvc.perform(get("/api/v1/repositories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }
}
