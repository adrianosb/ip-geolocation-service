package com.adriano.ip_geolocation_service.infrastructure.external;

import com.adriano.ip_geolocation_service.application.model.GeolocationInfo;
import com.adriano.ip_geolocation_service.application.port.GeolocationPort;
import com.adriano.ip_geolocation_service.infrastructure.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "app.geolocation.provider", havingValue = "ip-api", matchIfMissing = true)
public class IpApiGeolocationAdapter implements GeolocationPort {

    private static final Logger log = LoggerFactory.getLogger(IpApiGeolocationAdapter.class);

    private final HttpClient httpClient;
    private final AppProperties properties;
    private final ObjectMapper objectMapper;

    public IpApiGeolocationAdapter(HttpClient httpClient, AppProperties properties, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<GeolocationInfo> findByIp(String ip) {
        String url = properties.geolocation().apiUrl() + "/" + ip;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(properties.geolocation().timeoutSeconds()))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            IpApiResponse apiResponse = objectMapper.readValue(response.body(), IpApiResponse.class);

            if (!apiResponse.isSuccess()) {
                log.warn("ip-api.com returned status '{}' for IP {}", apiResponse.status(), ip);
                return Optional.empty();
            }

            return Optional.of(new GeolocationInfo(
                    apiResponse.query(),
                    apiResponse.countryCode(),
                    apiResponse.country(),
                    apiResponse.region(),
                    apiResponse.regionName(),
                    apiResponse.city(),
                    apiResponse.lat(),
                    apiResponse.lon(),
                    apiResponse.timezone(),
                    apiResponse.isp()));

        } catch (HttpTimeoutException e) {
            log.error("Timeout calling ip-api.com for IP {}: {}", ip, e.getMessage());
            return Optional.empty();
        } catch (ConnectException e) {
            log.error("Connection error calling ip-api.com for IP {}: {}", ip, e.getMessage());
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Request interrupted calling ip-api.com for IP {}", ip);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error calling ip-api.com for IP {}: {}", ip, e.getMessage());
            return Optional.empty();
        }
    }
}
