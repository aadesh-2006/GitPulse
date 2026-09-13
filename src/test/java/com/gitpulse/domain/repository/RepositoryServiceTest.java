package com.gitpulse.domain.repository;

import com.gitpulse.common.exception.DuplicateResourceException;
import com.gitpulse.common.exception.ResourceNotFoundException;
import com.gitpulse.domain.repository.dto.CreateRepositoryRequest;
import com.gitpulse.domain.repository.dto.RepositoryResponse;
import com.gitpulse.integration.github.client.GitHubRepositoryClient;
import com.gitpulse.integration.github.dto.GitHubRepositoryResponse;
import com.gitpulse.integration.github.exception.GitHubResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
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

    @Mock
    private GitHubRepositoryClient gitHubRepositoryClient;

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
    @DisplayName("Should successfully synchronize repository metadata with GitHub")
    void syncRepositoryWithGitHub_Success() {
        GitHubRepositoryResponse gitHubData = new GitHubRepositoryResponse();
        gitHubData.setId(987654L);
        gitHubData.setDescription("Updated Spring Boot description from GitHub");
        gitHubData.setDefaultBranch("main");
        gitHubData.setHtmlUrl("https://github.com/spring-projects/spring-boot");
        gitHubData.setLanguage("Java");
        gitHubData.setIsPrivate(false);
        gitHubData.setStargazersCount(72000);
        gitHubData.setForksCount(41000);
        gitHubData.setOpenIssuesCount(450);
        gitHubData.setPushedAt(Instant.parse("2026-09-01T10:00:00Z"));

        when(repositoryJpaRepository.findById(1L)).thenReturn(Optional.of(sampleRepository));
        when(gitHubRepositoryClient.getRepository("spring-projects", "spring-boot")).thenReturn(gitHubData);
        when(repositoryJpaRepository.save(any(Repository.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RepositoryResponse response = repositoryService.syncRepositoryWithGitHub(1L);

        assertThat(response).isNotNull();
        assertThat(response.getGithubId()).isEqualTo(987654L);
        assertThat(response.getDescription()).isEqualTo("Updated Spring Boot description from GitHub");
        assertThat(response.getHtmlUrl()).isEqualTo("https://github.com/spring-projects/spring-boot");
        assertThat(response.getPrimaryLanguage()).isEqualTo("Java");
        assertThat(response.isPrivate()).isFalse();
        assertThat(response.getStarsCount()).isEqualTo(72000);
        assertThat(response.getForksCount()).isEqualTo(41000);
        assertThat(response.getOpenIssuesCount()).isEqualTo(450);

        verify(gitHubRepositoryClient).getRepository("spring-projects", "spring-boot");
        verify(repositoryJpaRepository).save(sampleRepository);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when syncing nonexistent local repository")
    void syncRepositoryWithGitHub_LocalNotFound_ThrowsResourceNotFoundException() {
        when(repositoryJpaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> repositoryService.syncRepositoryWithGitHub(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Repository not found with id: '999'");

        verify(gitHubRepositoryClient, never()).getRepository(any(), any());
    }

    @Test
    @DisplayName("Should propagate GitHubResourceNotFoundException when GitHub repository is not found")
    void syncRepositoryWithGitHub_GitHubNotFound_ThrowsException() {
        when(repositoryJpaRepository.findById(1L)).thenReturn(Optional.of(sampleRepository));
        when(gitHubRepositoryClient.getRepository("spring-projects", "spring-boot"))
                .thenThrow(new GitHubResourceNotFoundException("spring-projects", "spring-boot"));

        assertThatThrownBy(() -> repositoryService.syncRepositoryWithGitHub(1L))
                .isInstanceOf(GitHubResourceNotFoundException.class)
                .hasMessageContaining("GitHub repository not found: 'spring-projects/spring-boot'");
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
