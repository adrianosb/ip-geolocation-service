package com.adriano.ip_geolocation_service.infrastructure.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
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

    public boolean isSuccess() {
        return "success".equals(status);
    }
}
