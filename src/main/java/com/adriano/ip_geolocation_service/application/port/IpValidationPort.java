package com.adriano.ip_geolocation_service.application.port;

public interface IpValidationPort {

    boolean isValidFormat(String ip);

    boolean isPrivateOrReserved(String ip);
}
