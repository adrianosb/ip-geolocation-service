package com.adriano.ip_geolocation_service.infrastructure.cache;

import com.adriano.ip_geolocation_service.application.model.GeolocationInfo;
import com.adriano.ip_geolocation_service.application.port.GeolocationCachePort;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class GeolocationCacheAdapter implements GeolocationCachePort {

    private final CacheManager cacheManager;

    public GeolocationCacheAdapter(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public Optional<GeolocationInfo> get(@NonNull String ip) {
        Cache cache = cacheManager.getCache("geolocation");
        if (cache == null)
            return Optional.empty();
        return Optional.ofNullable(cache.get(ip, GeolocationInfo.class));
    }

    @Override
    public void put(@NonNull String ip, GeolocationInfo info) {
        Cache cache = cacheManager.getCache("geolocation");
        if (cache != null) {
            cache.put(ip, info);
        }
    }
}
