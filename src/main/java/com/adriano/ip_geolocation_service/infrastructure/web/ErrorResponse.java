package com.adriano.ip_geolocation_service.infrastructure.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Error response body")
public record ErrorResponse(
        @Schema(description = "Error code", example = "INVALID_IP_FORMAT") String error,
        @Schema(description = "Human-readable error message", example = "Invalid IP address format") String message,
        @Schema(description = "Timestamp when the error occurred") Instant timestamp) {
}
