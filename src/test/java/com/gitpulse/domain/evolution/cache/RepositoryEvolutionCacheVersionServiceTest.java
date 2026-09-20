package com.gitpulse.domain.evolution.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryEvolutionCacheVersionServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RepositoryEvolutionCacheVersionService versionService;

    @BeforeEach
    void setUp() {
        versionService = new RepositoryEvolutionCacheVersionService(stringRedisTemplate);
    }

    @Test
    @DisplayName("getCurrentVersion should return default 1 when key is not present in Redis on initial request")
    void getCurrentVersion_WhenKeyMissing_ShouldReturnOne() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("gitpulse:evolution:version:42")).thenReturn(null);

        long version = versionService.getCurrentVersion(42L);

        assertThat(version).isEqualTo(1L);
    }

    @Test
    @DisplayName("getCurrentVersion should return parsed long when key exists")
    void getCurrentVersion_WhenKeyExists_ShouldReturnParsedLong() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("gitpulse:evolution:version:42")).thenReturn("5");

        long version = versionService.getCurrentVersion(42L);

        assertThat(version).isEqualTo(5L);
    }

    @Test
    @DisplayName("getCurrentVersion should gracefully fall back to 1 when Redis fails on read")
    void getCurrentVersion_WhenRedisThrowsException_ShouldReturnOne() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenThrow(new RedisConnectionFailureException("Connection refused"));

        long version = versionService.getCurrentVersion(42L);

        assertThat(version).isEqualTo(1L);
    }

    @Test
    @DisplayName("incrementVersion should transition version 1 -> 2 successfully")
    void incrementVersion_FromOneToTwo_ShouldReturnOptionalOfTwo() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("gitpulse:evolution:version:42")).thenReturn(2L);

        Optional<Long> newVersion = versionService.incrementVersion(42L);

        assertThat(newVersion).isPresent().contains(2L);
    }

    @Test
    @DisplayName("incrementVersion should transition version 2 -> 3 successfully")
    void incrementVersion_FromTwoToThree_ShouldReturnOptionalOfThree() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("gitpulse:evolution:version:42")).thenReturn(3L);

        Optional<Long> newVersion = versionService.incrementVersion(42L);

        assertThat(newVersion).isPresent().contains(3L);
    }

    @Test
    @DisplayName("incrementVersion should return Optional.empty and not throw or return fake 1 on Redis failure")
    void incrementVersion_WhenRedisThrowsException_ShouldReturnEmptyAndNotFakeOne() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenThrow(new RedisConnectionFailureException("Connection refused"));

        Optional<Long> result = versionService.incrementVersion(42L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Repository A version should remain independent from repository B")
    void versionsForDifferentRepositories_ShouldBeIndependent() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("gitpulse:evolution:version:10")).thenReturn("3");
        when(valueOperations.get("gitpulse:evolution:version:20")).thenReturn("7");

        long repoAVersion = versionService.getCurrentVersion(10L);
        long repoBVersion = versionService.getCurrentVersion(20L);

        assertThat(repoAVersion).isEqualTo(3L);
        assertThat(repoBVersion).isEqualTo(7L);
    }

    @Test
    @DisplayName("Methods should throw NullPointerException when repositoryId is null")
    void shouldThrowNpeOnNullRepositoryId() {
        assertThatThrownBy(() -> versionService.getCurrentVersion(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("repositoryId must not be null");

        assertThatThrownBy(() -> versionService.incrementVersion(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("repositoryId must not be null");
    }
}
