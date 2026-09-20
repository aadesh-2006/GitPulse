package com.gitpulse.config.cache;

import com.gitpulse.domain.evolution.cache.RepositoryEvolutionCacheVersionService;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Objects;

@Component("evolutionCacheKeyGenerator")
public class EvolutionCacheKeyGenerator implements KeyGenerator {

    private final RepositoryEvolutionCacheVersionService cacheVersionService;

    public EvolutionCacheKeyGenerator(RepositoryEvolutionCacheVersionService cacheVersionService) {
        this.cacheVersionService = Objects.requireNonNull(cacheVersionService, "cacheVersionService must not be null");
    }

    @Override
    public Object generate(Object target, Method method, Object... params) {
        String methodName = method.getName();

        if ("getRepositoryEvolution".equals(methodName) && params.length >= 3) {
            Long repositoryId = (Long) params[0];
            Instant from = (Instant) params[1];
            Instant to = (Instant) params[2];
            long version = cacheVersionService.getCurrentVersion(repositoryId);
            return "gitpulse:evolution:" + repositoryId + ":v" + version + ":" + from + ":" + to;
        }

        if ("compareRepositoryEvolution".equals(methodName) && params.length >= 5) {
            Long repositoryId = (Long) params[0];
            Instant currentFrom = (Instant) params[1];
            Instant currentTo = (Instant) params[2];
            Instant previousFrom = (Instant) params[3];
            Instant previousTo = (Instant) params[4];
            long version = cacheVersionService.getCurrentVersion(repositoryId);
            return "gitpulse:evolution:compare:" + repositoryId + ":v" + version + ":"
                    + currentFrom + ":" + currentTo + ":" + previousFrom + ":" + previousTo;
        }

        if ("getEvolutionComposition".equals(methodName) && params.length >= 3) {
            Long repositoryId = (Long) params[0];
            Instant from = (Instant) params[1];
            Instant to = (Instant) params[2];
            long version = cacheVersionService.getCurrentVersion(repositoryId);
            return "gitpulse:evolution:composition:" + repositoryId + ":v" + version + ":" + from + ":" + to;
        }

        // Fallback for any other methods
        StringBuilder sb = new StringBuilder("gitpulse:evolution:custom:");
        if (params.length > 0 && params[0] instanceof Long repoId) {
            long version = cacheVersionService.getCurrentVersion(repoId);
            sb.append(repoId).append(":v").append(version);
            for (int i = 1; i < params.length; i++) {
                sb.append(":").append(params[i]);
            }
        } else {
            for (Object param : params) {
                sb.append(":").append(param);
            }
        }
        return sb.toString();
    }
}
