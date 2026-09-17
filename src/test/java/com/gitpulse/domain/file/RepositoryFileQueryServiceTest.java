package com.gitpulse.domain.file;

import com.gitpulse.common.exception.AppException;
import com.gitpulse.common.exception.ResourceNotFoundException;
import com.gitpulse.domain.contributor.Contributor;
import com.gitpulse.domain.file.dto.RepositoryFileResponse;
import com.gitpulse.domain.repository.Repository;
import com.gitpulse.domain.repository.RepositoryJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryFileQueryServiceTest {

    @Mock
    private RepositoryJpaRepository repositoryJpaRepository;

    @Mock
    private RepositoryFileJpaRepository repositoryFileJpaRepository;

    private RepositoryFileQueryService queryService;
    private Repository repository;

    @BeforeEach
    void setUp() {
        queryService = new RepositoryFileQueryService(repositoryJpaRepository, repositoryFileJpaRepository);
        repository = new Repository("owner", "repo", "Description", "main");
        ReflectionTestUtils.setField(repository, "id", 1L);
    }

    @Test
    @DisplayName("Should return paginated repository file responses")
    void getRepositoryFiles_Success() {
        when(repositoryJpaRepository.existsById(1L)).thenReturn(true);

        Contributor contributor = new Contributor("dev@gitpulse.com", "dev", "Dev User");
        ReflectionTestUtils.setField(contributor, "id", 10L);

        Instant now = Instant.now();
        RepositoryFile file = new RepositoryFile(
                repository, "src/App.java", "App.java", "java", "src",
                5, 100, 20, 120, false, now, now, contributor
        );
        ReflectionTestUtils.setField(file, "id", 100L);

        Page<RepositoryFile> entityPage = new PageImpl<>(List.of(file), PageRequest.of(0, 20), 1);
        when(repositoryFileJpaRepository.findByRepositoryIdWithFilters(eq(1L), eq("java"), eq(false), any(Pageable.class)))
                .thenReturn(entityPage);

        Page<RepositoryFileResponse> result = queryService.getRepositoryFiles(
                1L, "java", false, PageRequest.of(0, 20, Sort.by("totalChurn").descending())
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        RepositoryFileResponse response = result.getContent().get(0);
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.repositoryId()).isEqualTo(1L);
        assertThat(response.filePath()).isEqualTo("src/App.java");
        assertThat(response.fileName()).isEqualTo("App.java");
        assertThat(response.extension()).isEqualTo("java");
        assertThat(response.directoryPath()).isEqualTo("src");
        assertThat(response.totalRevisions()).isEqualTo(5);
        assertThat(response.totalChurn()).isEqualTo(120);
        assertThat(response.isDeleted()).isFalse();
        assertThat(response.primaryContributor()).isNotNull();
        assertThat(response.primaryContributor().id()).isEqualTo(10L);
        assertThat(response.primaryContributor().email()).isEqualTo("dev@gitpulse.com");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when repository does not exist for file listing")
    void getRepositoryFiles_RepoNotFound() {
        when(repositoryJpaRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> queryService.getRepositoryFiles(999L, null, null, PageRequest.of(0, 20)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Repository not found with id: 999");
    }

    @Test
    @DisplayName("Should throw AppException when invalid sort field is requested")
    void getRepositoryFiles_InvalidSortField() {
        when(repositoryJpaRepository.existsById(1L)).thenReturn(true);

        Pageable invalidPageable = PageRequest.of(0, 20, Sort.by("arbitraryProperty").descending());

        assertThatThrownBy(() -> queryService.getRepositoryFiles(1L, null, null, invalidPageable))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Invalid sort field: 'arbitraryProperty'");
    }

    @Test
    @DisplayName("Should retrieve single file detail by path successfully")
    void getRepositoryFileByPath_Success() {
        Instant now = Instant.now();
        RepositoryFile file = new RepositoryFile(
                repository, "README.md", "README.md", "md", null,
                2, 20, 5, 25, false, now, now, null
        );
        ReflectionTestUtils.setField(file, "id", 101L);

        when(repositoryFileJpaRepository.findByRepositoryIdAndFilePath(1L, "README.md"))
                .thenReturn(Optional.of(file));

        RepositoryFileResponse response = queryService.getRepositoryFileByPath(1L, "README.md");

        assertThat(response.id()).isEqualTo(101L);
        assertThat(response.filePath()).isEqualTo("README.md");
        assertThat(response.primaryContributor()).isNull();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when file path does not exist in repository")
    void getRepositoryFileByPath_FileNotFound() {
        when(repositoryFileJpaRepository.findByRepositoryIdAndFilePath(1L, "unknown.txt"))
                .thenReturn(Optional.empty());
        when(repositoryJpaRepository.existsById(1L)).thenReturn(true);

        assertThatThrownBy(() -> queryService.getRepositoryFileByPath(1L, "unknown.txt"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("File not found with path 'unknown.txt' in repository id: 1");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when repository does not exist during file detail query")
    void getRepositoryFileByPath_RepoNotFound() {
        when(repositoryFileJpaRepository.findByRepositoryIdAndFilePath(999L, "unknown.txt"))
                .thenReturn(Optional.empty());
        when(repositoryJpaRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> queryService.getRepositoryFileByPath(999L, "unknown.txt"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Repository not found with id: 999");
    }

    @Test
    @DisplayName("Should retrieve hotspots delegating to getRepositoryFiles")
    void getRepositoryHotspots_Success() {
        when(repositoryJpaRepository.existsById(1L)).thenReturn(true);
        when(repositoryFileJpaRepository.findByRepositoryIdWithFilters(eq(1L), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(Page.empty());

        Page<RepositoryFileResponse> hotspots = queryService.getRepositoryHotspots(1L, null, null, PageRequest.of(0, 10));

        assertThat(hotspots).isEmpty();
        verify(repositoryFileJpaRepository).findByRepositoryIdWithFilters(eq(1L), eq(null), eq(null), any(Pageable.class));
    }
}