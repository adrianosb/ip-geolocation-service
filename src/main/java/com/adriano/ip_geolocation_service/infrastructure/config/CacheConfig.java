package com.adriano.ip_geolocation_service.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(AppProperties appProperties) {
        AppProperties.Cache cache = appProperties.cache();
        Caffeine<Object, Object> caffeineSpec = Caffeine.newBuilder()
                .maximumSize(cache.maxSize())
                .expireAfterWrite(cache.ttlHours(), TimeUnit.HOURS)
                .recordStats();
        CaffeineCacheManager manager = new CaffeineCacheManager("geolocation");
        manager.setCaffeine(Objects.requireNonNull(caffeineSpec));
        return manager;
    }
}
