package com.adriano.ip_geolocation_service.application.exception;

public class InvalidDevicePlatformException extends RuntimeException {

    public InvalidDevicePlatformException(String platform) {
        super("Invalid device platform: " + platform + ". Accepted values: iOS, Android, Web");
    }
}
