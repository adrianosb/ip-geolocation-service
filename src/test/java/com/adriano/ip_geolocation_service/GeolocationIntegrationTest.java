package com.adriano.ip_geolocation_service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.springframework.lang.NonNull;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GeolocationIntegrationTest {

    static final WireMockServer wireMockServer;

    static {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
    }

    @DynamicPropertySource
    static void overrideApiUrl(DynamicPropertyRegistry registry) {
        registry.add("app.geolocation.api-url",
                () -> "http://localhost:" + wireMockServer.port() + "/json");
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();
        var cache = cacheManager.getCache("geolocation");
        if (cache != null) {
            cache.clear();
        }
    }

    private @NonNull HttpHeaders headers(String platform) {
        HttpHeaders h = new HttpHeaders();
        h.set("x-device-platform", platform);
        return h;
    }

    private String successBody(String ip) {
        return """
                {
                  "status": "success",
                  "country": "United States",
                  "countryCode": "US",
                  "region": "VA",
                  "regionName": "Virginia",
                  "city": "Ashburn",
                  "lat": 39.03,
                  "lon": -77.5,
                  "timezone": "America/New_York",
                  "isp": "Google LLC",
                  "query": "%s"
                }
                """.formatted(ip);
    }

    @Test
    void valid_public_ip_returns_geolocation_data() {
        wireMockServer.stubFor(get(urlEqualTo("/json/8.8.8.8"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(successBody("8.8.8.8"))));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/geolocation/v1/locate?ip=8.8.8.8",
                HttpMethod.GET,
                new HttpEntity<>(headers("Web")),
                String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"ip\":\"8.8.8.8\"");
        assertThat(response.getBody()).contains("\"code\":\"US\"");
        assertThat(response.getBody()).contains("\"source\":\"api\"");
    }

    @Test
    void private_ip_returns_fallback_brazil() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/geolocation/v1/locate?ip=192.168.1.1",
                HttpMethod.GET,
                new HttpEntity<>(headers("Android")),
                String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"code\":\"BR\"");
        assertThat(response.getBody()).contains("\"source\":\"fallback\"");
        wireMockServer.verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void invalid_ip_returns_400_with_error_body() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/geolocation/v1/locate?ip=not-an-ip",
                HttpMethod.GET,
                new HttpEntity<>(headers("iOS")),
                String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).contains("\"error\":\"INVALID_IP_FORMAT\"");
    }

    @Test
    void api_down_returns_fallback_brazil() {
        wireMockServer.stubFor(get(anyUrl())
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/geolocation/v1/locate?ip=5.6.7.8",
                HttpMethod.GET,
                new HttpEntity<>(headers("Web")),
                String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"code\":\"BR\"");
        assertThat(response.getBody()).contains("\"source\":\"fallback\"");
    }

    @Test
    void second_request_same_ip_returns_from_cache() {
        wireMockServer.stubFor(get(urlEqualTo("/json/8.8.4.4"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(successBody("8.8.4.4"))));

        ResponseEntity<String> first = restTemplate.exchange(
                "/api/geolocation/v1/locate?ip=8.8.4.4",
                HttpMethod.GET,
                new HttpEntity<>(headers("iOS")),
                String.class);

        assertThat(first.getStatusCode().value()).isEqualTo(200);
        assertThat(first.getBody()).contains("\"source\":\"api\"");

        ResponseEntity<String> second = restTemplate.exchange(
                "/api/geolocation/v1/locate?ip=8.8.4.4",
                HttpMethod.GET,
                new HttpEntity<>(headers("iOS")),
                String.class);

        assertThat(second.getStatusCode().value()).isEqualTo(200);
        assertThat(second.getBody()).contains("\"source\":\"cache\"");
        wireMockServer.verify(1, getRequestedFor(urlEqualTo("/json/8.8.4.4")));
    }

    @Test
    void missing_platform_header_returns_400() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/geolocation/v1/locate?ip=8.8.8.8",
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).contains("\"error\":\"INVALID_DEVICE_PLATFORM\"");
    }
}
