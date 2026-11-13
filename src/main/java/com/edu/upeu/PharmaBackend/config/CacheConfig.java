package com.edu.upeu.PharmaBackend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
// Cache advice surrounds the transaction: put/evict runs only after a successful commit.
// Errors still pass through CacheErrorHandler, including failed invalidations.
@EnableCaching(order = Ordered.LOWEST_PRECEDENCE - 1)
public class CacheConfig implements CachingConfigurer {
    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    @Bean
    RedisCacheManagerBuilderCustomizer escriturasInmediatas(ObjectProvider<RedisConnectionFactory> conexiones) {
        // Spring Data Redis 4 defaults to asynchronous writes with Lettuce.
        // Catalogue reads after a write must observe its completed invalidation.
        return builder -> builder.cacheWriter(RedisCacheWriter.create(conexiones.getObject(),
                config -> config.immediateWrites()));
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler() {
            private void advertir(Cache cache, String operacion, RuntimeException error) {
                // Do not log cached values, connection strings or credentials.
                log.warn("Cache {} no disponible al {} | {}", cache.getName(), operacion,
                        error.getClass().getSimpleName());
            }

            @Override
            public void handleCacheGetError(RuntimeException error, Cache cache, Object key) {
                advertir(cache, "leer", error);
            }

            @Override
            public void handleCachePutError(RuntimeException error, Cache cache, Object key, Object value) {
                advertir(cache, "guardar", error);
            }

            @Override
            public void handleCacheEvictError(RuntimeException error, Cache cache, Object key) {
                advertir(cache, "invalidar", error);
            }

            @Override
            public void handleCacheClearError(RuntimeException error, Cache cache) {
                advertir(cache, "limpiar", error);
            }
        };
    }
}
