package com.adriano.ip_geolocation_service.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Bean
    @Profile("!redis")
    public CacheManager caffeineCacheManager(AppProperties appProperties) {
        AppProperties.Cache cache = appProperties.cache();
        Caffeine<Object, Object> caffeineSpec = Caffeine.newBuilder()
                .maximumSize(cache.maxSize())
                .expireAfterWrite(cache.ttlHours(), TimeUnit.HOURS)
                .recordStats();
        CaffeineCacheManager manager = new CaffeineCacheManager("geolocation");
        manager.setCaffeine(Objects.requireNonNull(caffeineSpec));
        return manager;
    }

    @Bean
    @Profile("redis")
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory, AppProperties appProperties) {
        Duration ttl = Objects.requireNonNull(Duration.ofHours(appProperties.cache().ttlHours()));
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));
        return RedisCacheManager.builder(Objects.requireNonNull(connectionFactory))
                .cacheDefaults(config)
                .build();
    }
}
