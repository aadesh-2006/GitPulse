package com.gitpulse.domain.evolution.cache;

import com.gitpulse.config.observability.GitPulseMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
public class RepositoryEvolutionCacheVersionService {

    private static final Logger log = LoggerFactory.getLogger(RepositoryEvolutionCacheVersionService.class);
    private static final String VERSION_KEY_PREFIX = "gitpulse:evolution:version:";
    private static final long DEFAULT_VERSION = 1L;

    private final StringRedisTemplate stringRedisTemplate;
    private final GitPulseMetrics gitPulseMetrics;

    public RepositoryEvolutionCacheVersionService(StringRedisTemplate stringRedisTemplate, GitPulseMetrics gitPulseMetrics) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.gitPulseMetrics = gitPulseMetrics;
    }

    public long getCurrentVersion(Long repositoryId) {
        Objects.requireNonNull(repositoryId, "repositoryId must not be null");

        if (stringRedisTemplate == null) {
            return DEFAULT_VERSION;
        }

        try {
            String key = VERSION_KEY_PREFIX + repositoryId;
            String val = stringRedisTemplate.opsForValue().get(key);
            if (val == null) {
                return DEFAULT_VERSION;
            }
            return Long.parseLong(val);
        } catch (Exception ex) {
            log.warn("Failed to retrieve evolution cache version for repository {}: {}. Falling back to default version {}",
                    repositoryId, ex.getMessage(), DEFAULT_VERSION);
            if (gitPulseMetrics != null) {
                gitPulseMetrics.recordCacheError(GitPulseMetrics.OPERATION_READ);
            }
            return DEFAULT_VERSION;
        }
    }

    public Optional<Long> incrementVersion(Long repositoryId) {
        Objects.requireNonNull(repositoryId, "repositoryId must not be null");

        if (stringRedisTemplate == null) {
            log.warn("StringRedisTemplate is unavailable. Cache invalidation skipped for repository {}", repositoryId);
            return Optional.empty();
        }

        try {
            String key = VERSION_KEY_PREFIX + repositoryId;
            Long newVersion = stringRedisTemplate.opsForValue().increment(key);
            log.info("Incremented evolution cache version for repository {} to {}", repositoryId, newVersion);
            return Optional.ofNullable(newVersion);
        } catch (Exception ex) {
            log.warn("Failed to increment evolution cache version for repository {}: {}. Redis cache invalidation not performed; relying on TTL expiration.",
                    repositoryId, ex.getMessage());
            if (gitPulseMetrics != null) {
                gitPulseMetrics.recordCacheError(GitPulseMetrics.OPERATION_WRITE);
            }
            return Optional.empty();
        }
    }
}
