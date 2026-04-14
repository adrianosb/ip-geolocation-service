package com.adriano.ip_geolocation_service.infrastructure.web;

import com.adriano.ip_geolocation_service.application.model.GeolocationResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Geolocation data for the queried IP address")
public record GeolocationResponseDto(
        @Schema(description = "The queried IP address", example = "8.8.8.8") String ip,
        @Schema(description = "Country information") Country country,
        @Schema(description = "Region information") Region region,
        @Schema(description = "City name", example = "São Paulo") String city,
        @Schema(description = "Geographic coordinates") Coordinates coordinates,
        @Schema(description = "Timezone identifier", example = "America/Sao_Paulo") String timezone,
        @Schema(description = "Internet service provider", example = "Telecom Provider") String isp,
        @Schema(description = "Data source: api, cache, or fallback", example = "api", allowableValues = {
                "api", "cache", "fallback" }) String source,
        @Schema(description = "Response timestamp") Instant timestamp) {

    @Schema(description = "Country details")
    public record Country(
            @Schema(description = "ISO 3166-1 alpha-2 country code", example = "BR") String code,
            @Schema(description = "Country name", example = "Brazil") String name) {
    }

    @Schema(description = "Region details")
    public record Region(
            @Schema(description = "Region code", example = "SP") String code,
            @Schema(description = "Region name", example = "São Paulo") String name) {
    }

    @Schema(description = "Geographic coordinates")
    public record Coordinates(
            @Schema(description = "Latitude", example = "-23.5505") Double latitude,
            @Schema(description = "Longitude", example = "-46.6333") Double longitude) {
    }

    public static GeolocationResponseDto from(GeolocationResponse response) {
        Country country = response.country() != null
                ? new Country(response.country().code(), response.country().name())
                : null;
        Region region = response.region() != null
                ? new Region(response.region().code(), response.region().name())
                : null;
        Coordinates coordinates = response.coordinates() != null
                ? new Coordinates(response.coordinates().latitude(), response.coordinates().longitude())
                : null;
        return new GeolocationResponseDto(
                response.ip(),
                country,
                region,
                response.city(),
                coordinates,
                response.timezone(),
                response.isp(),
                response.source(),
                response.timestamp());
    }
}
