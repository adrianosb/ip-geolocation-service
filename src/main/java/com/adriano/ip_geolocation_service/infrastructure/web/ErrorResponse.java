package com.adriano.ip_geolocation_service.infrastructure.web;

import java.time.Instant;

public record ErrorResponse(String error, String message, Instant timestamp) {
}
