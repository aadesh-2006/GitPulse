package com.gitpulse.domain.repository;

import com.gitpulse.common.exception.DuplicateResourceException;
import com.gitpulse.common.exception.ResourceNotFoundException;
import com.gitpulse.domain.repository.dto.CreateRepositoryRequest;
import com.gitpulse.domain.repository.dto.RepositoryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryServiceTest {

    @Mock
    private RepositoryJpaRepository repositoryJpaRepository;

    @InjectMocks
    private RepositoryService repositoryService;

    private Repository sampleRepository;

    @BeforeEach
    void setUp() {
        sampleRepository = new Repository("spring-projects", "spring-boot", "Spring Boot repository", "main");
        ReflectionTestUtils.setField(sampleRepository, "id", 1L);
    }

    @Test
    @DisplayName("Should successfully register a new repository")
    void createRepository_Success() {
        CreateRepositoryRequest request = new CreateRepositoryRequest("spring-projects", "spring-boot", "Spring Boot repository", "main");

        when(repositoryJpaRepository.existsByOwnerAndName("spring-projects", "spring-boot")).thenReturn(false);
        when(repositoryJpaRepository.save(any(Repository.class))).thenReturn(sampleRepository);

        RepositoryResponse response = repositoryService.createRepository(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getOwner()).isEqualTo("spring-projects");
        assertThat(response.getName()).isEqualTo("spring-boot");
        assertThat(response.getFullName()).isEqualTo("spring-projects/spring-boot");
        assertThat(response.getDefaultBranch()).isEqualTo("main");

        verify(repositoryJpaRepository).existsByOwnerAndName("spring-projects", "spring-boot");
        verify(repositoryJpaRepository).save(any(Repository.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when repository already exists")
    void createRepository_Duplicate_ThrowsDuplicateResourceException() {
        CreateRepositoryRequest request = new CreateRepositoryRequest("spring-projects", "spring-boot");

        when(repositoryJpaRepository.existsByOwnerAndName("spring-projects", "spring-boot")).thenReturn(true);

        assertThatThrownBy(() -> repositoryService.createRepository(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Repository already exists with fullName: 'spring-projects/spring-boot'");

        verify(repositoryJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully retrieve repository by ID")
    void getRepositoryById_Success() {
        when(repositoryJpaRepository.findById(1L)).thenReturn(Optional.of(sampleRepository));

        RepositoryResponse response = repositoryService.getRepositoryById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getFullName()).isEqualTo("spring-projects/spring-boot");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when repository ID not found")
    void getRepositoryById_NotFound_ThrowsResourceNotFoundException() {
        when(repositoryJpaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> repositoryService.getRepositoryById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Repository not found with id: '999'");
    }

    @Test
    @DisplayName("Should return all registered repositories")
    void getAllRepositories_Success() {
        Repository anotherRepo = new Repository("google", "guava", "Guava core libraries", "master");
        ReflectionTestUtils.setField(anotherRepo, "id", 2L);

        when(repositoryJpaRepository.findAll()).thenReturn(List.of(sampleRepository, anotherRepo));

        List<RepositoryResponse> results = repositoryService.getAllRepositories();

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getFullName()).isEqualTo("spring-projects/spring-boot");
        assertThat(results.get(1).getFullName()).isEqualTo("google/guava");
    }
}
