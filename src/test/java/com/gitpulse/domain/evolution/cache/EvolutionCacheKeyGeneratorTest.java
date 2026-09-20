package com.gitpulse.domain.evolution.cache;

import com.gitpulse.config.cache.EvolutionCacheKeyGenerator;
import com.gitpulse.domain.evolution.RepositoryEvolutionComparisonQueryService;
import com.gitpulse.domain.evolution.RepositoryEvolutionCompositionQueryService;
import com.gitpulse.domain.evolution.RepositoryEvolutionQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvolutionCacheKeyGeneratorTest {

    @Mock
    private RepositoryEvolutionCacheVersionService versionService;

    private EvolutionCacheKeyGenerator keyGenerator;

    @BeforeEach
    void setUp() {
        keyGenerator = new EvolutionCacheKeyGenerator(versionService);
    }

    @Test
    @DisplayName("generate should create correct key for getRepositoryEvolution")
    void generate_ForRepositoryEvolution_ShouldProduceExpectedKey() throws NoSuchMethodException {
        Long repoId = 10L;
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-06-01T00:00:00Z");
        when(versionService.getCurrentVersion(repoId)).thenReturn(1L);

        Method method = RepositoryEvolutionQueryService.class.getMethod("getRepositoryEvolution", Long.class, Instant.class, Instant.class);
        Object key = keyGenerator.generate(null, method, repoId, from, to);

        assertThat(key).isEqualTo("gitpulse:evolution:10:v1:2026-01-01T00:00:00Z:2026-06-01T00:00:00Z");
    }

    @Test
    @DisplayName("generate should create correct key for compareRepositoryEvolution")
    void generate_ForRepositoryEvolutionComparison_ShouldProduceExpectedKey() throws NoSuchMethodException {
        Long repoId = 15L;
        Instant currentFrom = Instant.parse("2026-04-01T00:00:00Z");
        Instant currentTo = Instant.parse("2026-07-01T00:00:00Z");
        Instant previousFrom = Instant.parse("2026-01-01T00:00:00Z");
        Instant previousTo = Instant.parse("2026-04-01T00:00:00Z");
        when(versionService.getCurrentVersion(repoId)).thenReturn(2L);

        Method method = RepositoryEvolutionComparisonQueryService.class.getMethod(
                "compareRepositoryEvolution", Long.class, Instant.class, Instant.class, Instant.class, Instant.class);
        Object key = keyGenerator.generate(null, method, repoId, currentFrom, currentTo, previousFrom, previousTo);

        assertThat(key).isEqualTo("gitpulse:evolution:compare:15:v2:2026-04-01T00:00:00Z:2026-07-01T00:00:00Z:2026-01-01T00:00:00Z:2026-04-01T00:00:00Z");
    }

    @Test
    @DisplayName("generate should create correct key for getEvolutionComposition")
    void generate_ForRepositoryEvolutionComposition_ShouldProduceExpectedKey() throws NoSuchMethodException {
        Long repoId = 20L;
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-03-01T00:00:00Z");
        when(versionService.getCurrentVersion(repoId)).thenReturn(3L);

        Method method = RepositoryEvolutionCompositionQueryService.class.getMethod("getEvolutionComposition", Long.class, Instant.class, Instant.class);
        Object key = keyGenerator.generate(null, method, repoId, from, to);

        assertThat(key).isEqualTo("gitpulse:evolution:composition:20:v3:2026-01-01T00:00:00Z:2026-03-01T00:00:00Z");
    }
}
