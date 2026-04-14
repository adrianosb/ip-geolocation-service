package com.adriano.ip_geolocation_service.application.port;

import com.adriano.ip_geolocation_service.application.model.GeolocationInfo;
import org.springframework.lang.NonNull;

import java.util.Optional;

public interface GeolocationCachePort {
    Optional<GeolocationInfo> get(@NonNull String ip);

    void put(@NonNull String ip, GeolocationInfo info);
}
