package com.adriano.ip_geolocation_service.infrastructure.web;

import com.adriano.ip_geolocation_service.application.exception.InvalidDevicePlatformException;
import com.adriano.ip_geolocation_service.application.exception.InvalidIpAddressException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidIpAddressException.class)
    public ResponseEntity<ErrorResponse> handleInvalidIp(InvalidIpAddressException ex) {
        return ResponseEntity.badRequest().body(
                new ErrorResponse("INVALID_IP_FORMAT", "Invalid IP address format", Instant.now()));
    }

    @ExceptionHandler(InvalidDevicePlatformException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPlatform(InvalidDevicePlatformException ex) {
        return ResponseEntity.badRequest().body(
                new ErrorResponse("INVALID_DEVICE_PLATFORM",
                        "Invalid or missing x-device-platform header. Accepted values: iOS, Android, Web",
                        Instant.now()));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
        if ("x-device-platform".equalsIgnoreCase(ex.getHeaderName())) {
            return ResponseEntity.badRequest().body(
                    new ErrorResponse("INVALID_DEVICE_PLATFORM",
                            "Invalid or missing x-device-platform header. Accepted values: iOS, Android, Web",
                            Instant.now()));
        }
        return ResponseEntity.badRequest().body(
                new ErrorResponse("MISSING_HEADER", "Required header is missing: " + ex.getHeaderName(),
                        Instant.now()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest().body(
                new ErrorResponse("MISSING_PARAMETER", "Required parameter is missing: " + ex.getParameterName(),
                        Instant.now()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        logger.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred", Instant.now()));
    }
}
