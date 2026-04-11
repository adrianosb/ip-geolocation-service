package com.adriano.ip_geolocation_service.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Geolocation geolocation, Fallback fallback, Cache cache) {

    public record Geolocation(String apiUrl, int timeoutSeconds) {
    }

    public record Fallback(Country country) {
        public record Country(String code, String name) {
        }
    }

    public record Cache(int ttlHours, int maxSize) {
    }
}
