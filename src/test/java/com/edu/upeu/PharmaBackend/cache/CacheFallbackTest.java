package com.edu.upeu.PharmaBackend.cache;

import com.edu.upeu.PharmaBackend.config.CacheConfig;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CacheFallbackTest {
    @Configuration
    @Import(CacheConfig.class)
    static class Config {
        @Bean CacheManager cacheManager() {
            Cache cache = mock(Cache.class);
            when(cache.getName()).thenReturn("categorias");
            when(cache.get(any())).thenThrow(new IllegalStateException("Redis unavailable"));
            doThrow(new IllegalStateException("Redis unavailable")).when(cache).put(any(), any());
            doThrow(new IllegalStateException("Redis unavailable")).when(cache).clear();
            CacheManager manager = mock(CacheManager.class);
            when(manager.getCache("categorias")).thenReturn(cache);
            return manager;
        }
        @Bean Catalogo catalogo() { return new Catalogo(); }
    }

    static class Catalogo {
        int lecturas;
        @Cacheable(cacheNames = "categorias", key = "'todas'")
        public String leer() { lecturas++; return "datos de Oracle"; }
        @CacheEvict(cacheNames = "categorias", allEntries = true)
        public void escribir() {}
    }

    @Test
    void redisCaidoNoImpideLeerNiEscribir() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            Catalogo catalogo = context.getBean(Catalogo.class);
            assertThat(catalogo.leer()).isEqualTo("datos de Oracle");
            assertThat(catalogo.leer()).isEqualTo("datos de Oracle");
            assertThatCode(catalogo::escribir).doesNotThrowAnyException();
        }
    }
}
