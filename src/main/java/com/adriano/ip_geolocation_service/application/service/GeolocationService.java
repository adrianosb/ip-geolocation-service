package com.adriano.ip_geolocation_service.application.service;

import com.adriano.ip_geolocation_service.application.exception.InvalidIpAddressException;
import com.adriano.ip_geolocation_service.application.model.FallbackCountry;
import com.adriano.ip_geolocation_service.application.model.GeolocationInfo;
import com.adriano.ip_geolocation_service.application.model.GeolocationResponse;
import com.adriano.ip_geolocation_service.application.port.GeolocationCachePort;
import com.adriano.ip_geolocation_service.application.port.GeolocationPort;
import com.adriano.ip_geolocation_service.application.port.GeolocationUseCase;
import com.adriano.ip_geolocation_service.application.port.IpValidationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class GeolocationService implements GeolocationUseCase {

    private static final Logger logger = LoggerFactory.getLogger(GeolocationService.class);

    private final GeolocationPort geolocationPort;
    private final IpValidationPort validator;
    private final GeolocationCachePort cache;
    private final FallbackCountry fallbackCountry;

    public GeolocationService(GeolocationPort geolocationPort,
            IpValidationPort validator,
            GeolocationCachePort cache,
            FallbackCountry fallbackCountry) {
        this.geolocationPort = geolocationPort;
        this.validator = validator;
        this.cache = cache;
        this.fallbackCountry = fallbackCountry;
    }

    @Override
    public GeolocationResponse locate(@NonNull String ip) {
        if (!validator.isValidFormat(ip)) {
            throw new InvalidIpAddressException(ip);
        }

        if (validator.isPrivateOrReserved(ip)) {
            logger.debug("IP {} is private or reserved. Returning fallback.", ip);
            return buildFallback(ip);
        }

        Optional<GeolocationInfo> cached = cache.get(ip);
        if (cached.isPresent()) {
            logger.debug("Cache hit for IP {}.", ip);
            return buildResponse(cached.get(), "cache");
        }

        Optional<GeolocationInfo> result = geolocationPort.findByIp(ip);
        if (result.isEmpty()) {
            logger.warn("Geolocation API returned no data for IP {}. Returning fallback.", ip);
            return buildFallback(ip);
        }

        GeolocationInfo info = result.get();
        cache.put(ip, info);
        return buildResponse(info, "api");
    }

    private GeolocationResponse buildResponse(GeolocationInfo info, String source) {
        return new GeolocationResponse(
                info.ip(),
                new GeolocationResponse.Country(info.countryCode(), info.countryName()),
                new GeolocationResponse.Region(info.regionCode(), info.regionName()),
                info.city(),
                new GeolocationResponse.Coordinates(info.latitude(), info.longitude()),
                info.timezone(),
                info.isp(),
                source,
                Instant.now());
    }

    private GeolocationResponse buildFallback(String ip) {
        return new GeolocationResponse(
                ip,
                new GeolocationResponse.Country(fallbackCountry.code(), fallbackCountry.name()),
                null,
                null,
                null,
                null,
                null,
                "fallback",
                Instant.now());
    }
}
