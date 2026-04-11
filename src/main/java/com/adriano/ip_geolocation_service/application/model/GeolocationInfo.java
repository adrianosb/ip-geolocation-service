package com.adriano.ip_geolocation_service.application.model;

public record GeolocationInfo(
                String ip,
                String countryCode,
                String countryName,
                String regionCode,
                String regionName,
                String city,
                Double latitude,
                Double longitude,
                String timezone,
                String isp) {
}
