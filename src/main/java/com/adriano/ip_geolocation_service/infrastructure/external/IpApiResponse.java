package com.adriano.ip_geolocation_service.infrastructure.external;

public record IpApiResponse(
        String status,
        String country,
        String countryCode,
        String region,
        String regionName,
        String city,
        Double lat,
        Double lon,
        String timezone,
        String isp,
        String query) {
}
