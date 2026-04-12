package com.adriano.ip_geolocation_service.application.service;

import com.adriano.ip_geolocation_service.application.exception.InvalidIpAddressException;
import com.adriano.ip_geolocation_service.application.model.GeolocationInfo;
import com.adriano.ip_geolocation_service.application.model.GeolocationResponse;
import com.adriano.ip_geolocation_service.application.port.GeolocationPort;
import com.adriano.ip_geolocation_service.infrastructure.config.AppProperties;
import com.adriano.ip_geolocation_service.infrastructure.validation.IpAddressValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class GeolocationService {

    private static final Logger logger = LoggerFactory.getLogger(GeolocationService.class);

    private final GeolocationPort geolocationPort;
    private final IpAddressValidator validator;
    private final CacheManager cacheManager;
    private final AppProperties properties;

    public GeolocationService(GeolocationPort geolocationPort,
            IpAddressValidator validator,
            CacheManager cacheManager,
            AppProperties properties) {
        this.geolocationPort = geolocationPort;
        this.validator = validator;
        this.cacheManager = cacheManager;
        this.properties = properties;
    }

    public GeolocationResponse locate(@NonNull String ip) {
        if (!validator.isValidFormat(ip)) {
            throw new InvalidIpAddressException(ip);
        }

        if (validator.isPrivateOrReserved(ip)) {
            logger.debug("IP {} is private or reserved. Returning fallback.", ip);
            return buildFallback(ip);
        }

        Cache cache = cacheManager.getCache("geolocation");
        GeolocationInfo cached = cache != null ? cache.get(ip, GeolocationInfo.class) : null;
        if (cached != null) {
            logger.debug("Cache hit for IP {}.", ip);
            return buildResponse(cached, "cache");
        }

        Optional<GeolocationInfo> result = geolocationPort.findByIp(ip);
        if (result.isEmpty()) {
            logger.warn("Geolocation API returned no data for IP {}. Returning fallback.", ip);
            return buildFallback(ip);
        }

        GeolocationInfo info = result.get();
        if (cache != null) {
            cache.put(ip, info);
        }
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
                new GeolocationResponse.Country(
                        properties.fallback().country().code(),
                        properties.fallback().country().name()),
                null,
                null,
                null,
                null,
                null,
                "fallback",
                Instant.now());
    }
}
