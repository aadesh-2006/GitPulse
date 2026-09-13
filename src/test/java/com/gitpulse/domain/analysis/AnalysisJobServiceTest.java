package com.gitpulse.domain.analysis;

import com.gitpulse.common.exception.ResourceNotFoundException;
import com.gitpulse.domain.analysis.dto.AnalysisJobResponse;
import com.gitpulse.domain.repository.Repository;
import com.gitpulse.domain.repository.RepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisJobServiceTest {

    @Mock
    private AnalysisJobJpaRepository analysisJobJpaRepository;

    @Mock
    private RepositoryService repositoryService;

    @InjectMocks
    private AnalysisJobService analysisJobService;

    private Repository sampleRepository;
    private AnalysisJob sampleJob;

    @BeforeEach
    void setUp() {
        sampleRepository = new Repository("spring-projects", "spring-boot", "Spring Boot repo", "main");
        ReflectionTestUtils.setField(sampleRepository, "id", 10L);

        sampleJob = new AnalysisJob(sampleRepository, AnalysisJobStatus.PENDING);
        ReflectionTestUtils.setField(sampleJob, "id", 100L);
    }

    @Test
    @DisplayName("Should successfully create a PENDING analysis job for an existing repository")
    void createAnalysisJob_Success() {
        when(repositoryService.findEntityById(10L)).thenReturn(sampleRepository);
        when(analysisJobJpaRepository.save(any(AnalysisJob.class))).thenReturn(sampleJob);

        AnalysisJobResponse response = analysisJobService.createAnalysisJob(10L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getRepositoryId()).isEqualTo(10L);
        assertThat(response.getRepositoryFullName()).isEqualTo("spring-projects/spring-boot");
        assertThat(response.getStatus()).isEqualTo(AnalysisJobStatus.PENDING);

        verify(repositoryService).findEntityById(10L);
        verify(analysisJobJpaRepository).save(any(AnalysisJob.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when creating analysis job for nonexistent repository")
    void createAnalysisJob_NonexistentRepository_ThrowsResourceNotFoundException() {
        when(repositoryService.findEntityById(999L))
                .thenThrow(new ResourceNotFoundException("Repository", "id", 999L));

        assertThatThrownBy(() -> analysisJobService.createAnalysisJob(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Repository not found with id: '999'");

        verify(analysisJobJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully retrieve analysis job by ID")
    void getAnalysisJobById_Success() {
        when(analysisJobJpaRepository.findById(100L)).thenReturn(Optional.of(sampleJob));

        AnalysisJobResponse response = analysisJobService.getAnalysisJobById(100L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getStatus()).isEqualTo(AnalysisJobStatus.PENDING);
        assertThat(response.getRepositoryId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when analysis job ID not found")
    void getAnalysisJobById_NotFound_ThrowsResourceNotFoundException() {
        when(analysisJobJpaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> analysisJobService.getAnalysisJobById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("AnalysisJob not found with id: '999'");
    }
}
