package com.gitpulse.config.cache;

import com.gitpulse.config.observability.GitPulseMetrics;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisCacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheConfig.class);

    public static final String CACHE_EVOLUTION = "repositoryEvolution";
    public static final String CACHE_EVOLUTION_COMPARISON = "repositoryEvolutionComparison";
    public static final String CACHE_EVOLUTION_COMPOSITION = "repositoryEvolutionComposition";

    @Value("${spring.cache.redis.time-to-live:5m}")
    private Duration defaultTtl;

    private final GitPulseMetrics gitPulseMetrics;

    public RedisCacheConfig() {
        this(null);
    }

    public RedisCacheConfig(GitPulseMetrics gitPulseMetrics) {
        this.gitPulseMetrics = gitPulseMetrics;
    }

    public ObjectMapper createRedisObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();
        objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.EVERYTHING, JsonTypeInfo.As.PROPERTY);

        return objectMapper;
    }

    @Bean
    public RedisCacheConfiguration defaultCacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(defaultTtl != null ? defaultTtl : Duration.ofMinutes(5))
                .disableCachingNullValues()
                .computePrefixWith(cacheName -> "")
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(createRedisObjectMapper())));
    }

    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory, RedisCacheConfiguration defaultCacheConfiguration) {
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfiguration)
                .withCacheConfiguration(CACHE_EVOLUTION, defaultCacheConfiguration)
                .withCacheConfiguration(CACHE_EVOLUTION_COMPARISON, defaultCacheConfiguration)
                .withCacheConfiguration(CACHE_EVOLUTION_COMPOSITION, defaultCacheConfiguration)
                .build();
    }

    @Override
    @Bean
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis cache GET error for key [{}] in cache [{}]: {}. Falling back to database.",
                        key, cache != null ? cache.getName() : "unknown", exception.getMessage());
                if (gitPulseMetrics != null) {
                    gitPulseMetrics.recordCacheError(GitPulseMetrics.OPERATION_READ);
                }
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Redis cache PUT error for key [{}] in cache [{}]: {}. Continuing without caching.",
                        key, cache != null ? cache.getName() : "unknown", exception.getMessage());
                if (gitPulseMetrics != null) {
                    gitPulseMetrics.recordCacheError(GitPulseMetrics.OPERATION_WRITE);
                }
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis cache EVICT error for key [{}] in cache [{}]: {}.",
                        key, cache != null ? cache.getName() : "unknown", exception.getMessage());
                if (gitPulseMetrics != null) {
                    gitPulseMetrics.recordCacheError(GitPulseMetrics.OPERATION_WRITE);
                }
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Redis cache CLEAR error for cache [{}]: {}.",
                        cache != null ? cache.getName() : "unknown", exception.getMessage());
                if (gitPulseMetrics != null) {
                    gitPulseMetrics.recordCacheError(GitPulseMetrics.OPERATION_WRITE);
                }
            }
        };
    }
}
