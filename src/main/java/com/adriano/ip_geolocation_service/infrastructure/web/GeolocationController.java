package com.adriano.ip_geolocation_service.infrastructure.web;

import com.adriano.ip_geolocation_service.application.exception.InvalidDevicePlatformException;
import com.adriano.ip_geolocation_service.application.model.GeolocationResponse;
import com.adriano.ip_geolocation_service.application.service.GeolocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/geolocation/v1")
@Tag(name = "Geolocation", description = "Resolve geographic location from an IP address")
public class GeolocationController {

    private static final Set<String> VALID_PLATFORMS = Set.of("iOS", "Android", "Web");

    private final GeolocationService geolocationService;

    public GeolocationController(GeolocationService geolocationService) {
        this.geolocationService = geolocationService;
    }

    @Operation(summary = "Locate an IP address", description = "Returns geographic information for the given IP. Falls back to Brazil if the external API is unavailable.")
    @ApiResponse(responseCode = "200", description = "Location resolved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeolocationResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping(value = "/locate", produces = "application/json")
    public ResponseEntity<GeolocationResponse> locate(
            @Parameter(description = "IPv4 or IPv6 address to locate", required = true, example = "8.8.8.8") @RequestParam @NonNull String ip,
            @Parameter(description = "Device platform. Accepted values: iOS, Android, Web", required = true, example = "Web") @RequestHeader("x-device-platform") String devicePlatform) {

        if (!VALID_PLATFORMS.contains(devicePlatform)) {
            throw new InvalidDevicePlatformException(devicePlatform);
        }

        GeolocationResponse response = geolocationService.locate(ip);
        return ResponseEntity.ok(response);
    }
}
