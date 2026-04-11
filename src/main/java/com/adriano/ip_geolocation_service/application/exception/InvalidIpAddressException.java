package com.adriano.ip_geolocation_service.application.exception;

public class InvalidIpAddressException extends RuntimeException {

    public InvalidIpAddressException(String ip) {
        super("Invalid IP address format: " + ip);
    }
}
