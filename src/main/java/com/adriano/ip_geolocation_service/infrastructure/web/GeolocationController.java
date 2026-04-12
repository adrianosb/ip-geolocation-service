package com.adriano.ip_geolocation_service.infrastructure.web;

import com.adriano.ip_geolocation_service.application.exception.InvalidDevicePlatformException;
import com.adriano.ip_geolocation_service.application.model.GeolocationResponse;
import com.adriano.ip_geolocation_service.application.service.GeolocationService;
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
public class GeolocationController {

    private static final Set<String> VALID_PLATFORMS = Set.of("iOS", "Android", "Web");

    private final GeolocationService geolocationService;

    public GeolocationController(GeolocationService geolocationService) {
        this.geolocationService = geolocationService;
    }

    @GetMapping("/locate")
    public ResponseEntity<GeolocationResponse> locate(
            @RequestParam @NonNull String ip,
            @RequestHeader("x-device-platform") String devicePlatform) {

        if (!VALID_PLATFORMS.contains(devicePlatform)) {
            throw new InvalidDevicePlatformException(devicePlatform);
        }

        GeolocationResponse response = geolocationService.locate(ip);
        return ResponseEntity.ok(response);
    }
}
