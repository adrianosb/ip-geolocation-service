package com.adriano.ip_geolocation_service.application.model;

import java.time.Instant;

public record GeolocationResponse(
        String ip,
        Country country,
        Region region,
        String city,
        Coordinates coordinates,
        String timezone,
        String isp,
        String source,
        Instant timestamp) {
    public record Country(String code, String name) {
    }

    public record Region(String code, String name) {
    }

    public record Coordinates(Double latitude, Double longitude) {
    }
}
