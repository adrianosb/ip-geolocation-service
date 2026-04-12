package com.adriano.ip_geolocation_service.application.port;

import com.adriano.ip_geolocation_service.application.model.GeolocationInfo;

import java.util.Optional;

public interface GeolocationPort {

    Optional<GeolocationInfo> findByIp(String ip);
}
