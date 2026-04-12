package com.adriano.ip_geolocation_service.infrastructure.external;

import com.adriano.ip_geolocation_service.application.model.GeolocationInfo;
import com.adriano.ip_geolocation_service.infrastructure.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

class IpApiGeolocationAdapterTest {

    private static WireMockServer wireMockServer;
    private IpApiGeolocationAdapter adapter;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();

        String apiUrl = "http://localhost:" + wireMockServer.port() + "/json";
        AppProperties properties = new AppProperties(
                new AppProperties.Geolocation("ip-api", apiUrl, 1),
                new AppProperties.Fallback(new AppProperties.Fallback.Country("BR", "Brazil")),
                new AppProperties.Cache(24, 10000));

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        adapter = new IpApiGeolocationAdapter(httpClient, properties, new ObjectMapper());
    }

    @Test
    void returns_geolocation_info_when_api_returns_success() {
        String ip = "8.8.8.8";
        wireMockServer.stubFor(get(urlEqualTo("/json/" + ip))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
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
                                  "query": "8.8.8.8"
                                }
                                """)));

        Optional<GeolocationInfo> result = adapter.findByIp(ip);

        assertThat(result).isPresent();
        assertThat(result.get().ip()).isEqualTo("8.8.8.8");
        assertThat(result.get().countryCode()).isEqualTo("US");
        assertThat(result.get().countryName()).isEqualTo("United States");
        assertThat(result.get().regionCode()).isEqualTo("VA");
        assertThat(result.get().regionName()).isEqualTo("Virginia");
        assertThat(result.get().city()).isEqualTo("Ashburn");
        assertThat(result.get().latitude()).isEqualTo(39.03);
        assertThat(result.get().isp()).isEqualTo("Google LLC");
        assertThat(result.get().timezone()).isEqualTo("America/New_York");
    }

    @Test
    void returns_empty_when_api_returns_fail_status() {
        String ip = "1.2.3.4";
        wireMockServer.stubFor(get(urlEqualTo("/json/" + ip))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"status": "fail", "message": "private range", "query": "1.2.3.4"}
                                """)));

        Optional<GeolocationInfo> result = adapter.findByIp(ip);

        assertThat(result).isEmpty();
    }

    @Test
    void returns_empty_on_request_timeout() {
        String ip = "8.8.8.8";
        wireMockServer.stubFor(get(urlEqualTo("/json/" + ip))
                .willReturn(aResponse()
                        .withFixedDelay(3000)
                        .withBody("{}")));

        Optional<GeolocationInfo> result = adapter.findByIp(ip);

        assertThat(result).isEmpty();
    }

    @Test
    void returns_empty_on_http_500() {
        String ip = "8.8.8.8";
        wireMockServer.stubFor(get(urlEqualTo("/json/" + ip))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        Optional<GeolocationInfo> result = adapter.findByIp(ip);

        assertThat(result).isEmpty();
    }

    @Test
    void returns_empty_on_invalid_json() {
        String ip = "8.8.8.8";
        wireMockServer.stubFor(get(urlEqualTo("/json/" + ip))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("not-valid-json")));

        Optional<GeolocationInfo> result = adapter.findByIp(ip);

        assertThat(result).isEmpty();
    }

    @Test
    void returns_geolocation_info_for_brazilian_ip() {
        String ip = "177.45.123.45";
        wireMockServer.stubFor(get(urlEqualTo("/json/" + ip))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "status": "success",
                                  "country": "Brazil",
                                  "countryCode": "BR",
                                  "region": "SP",
                                  "regionName": "São Paulo",
                                  "city": "São Paulo",
                                  "lat": -23.5505,
                                  "lon": -46.6333,
                                  "timezone": "America/Sao_Paulo",
                                  "isp": "Claro S.A.",
                                  "query": "177.45.123.45"
                                }
                                """)));

        Optional<GeolocationInfo> result = adapter.findByIp(ip);

        assertThat(result).isPresent();
        assertThat(result.get().ip()).isEqualTo("177.45.123.45");
        assertThat(result.get().countryCode()).isEqualTo("BR");
        assertThat(result.get().countryName()).isEqualTo("Brazil");
        assertThat(result.get().regionCode()).isEqualTo("SP");
        assertThat(result.get().regionName()).isEqualTo("São Paulo");
        assertThat(result.get().city()).isEqualTo("São Paulo");
        assertThat(result.get().latitude()).isEqualTo(-23.5505);
        assertThat(result.get().longitude()).isEqualTo(-46.6333);
        assertThat(result.get().timezone()).isEqualTo("America/Sao_Paulo");
        assertThat(result.get().isp()).isEqualTo("Claro S.A.");
    }

    @Test
    void returns_geolocation_info_for_another_brazilian_region() {
        String ip = "200.195.32.10";
        wireMockServer.stubFor(get(urlEqualTo("/json/" + ip))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "status": "success",
                                  "country": "Brazil",
                                  "countryCode": "BR",
                                  "region": "RJ",
                                  "regionName": "Rio de Janeiro",
                                  "city": "Rio de Janeiro",
                                  "lat": -22.9068,
                                  "lon": -43.1729,
                                  "timezone": "America/Sao_Paulo",
                                  "isp": "Oi S.A.",
                                  "query": "200.195.32.10"
                                }
                                """)));

        Optional<GeolocationInfo> result = adapter.findByIp(ip);

        assertThat(result).isPresent();
        assertThat(result.get().countryCode()).isEqualTo("BR");
        assertThat(result.get().regionCode()).isEqualTo("RJ");
        assertThat(result.get().city()).isEqualTo("Rio de Janeiro");
        assertThat(result.get().latitude()).isEqualTo(-22.9068);
        assertThat(result.get().longitude()).isEqualTo(-43.1729);
    }

    @Test
    void returns_geolocation_info_for_ipv6_google() {
        String ip = "2001:4860:4860::8888";
        wireMockServer.stubFor(get(urlEqualTo("/json/" + ip))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
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
                                  "query": "2001:4860:4860::8888"
                                }
                                """)));

        Optional<GeolocationInfo> result = adapter.findByIp(ip);

        assertThat(result).isPresent();
        assertThat(result.get().ip()).isEqualTo("2001:4860:4860::8888");
        assertThat(result.get().countryCode()).isEqualTo("US");
        assertThat(result.get().isp()).isEqualTo("Google LLC");
    }

    @Test
    void returns_geolocation_info_for_ipv6_brazilian() {
        String ip = "2804:14d:baa3:9a00::1";
        wireMockServer.stubFor(get(urlEqualTo("/json/" + ip))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "status": "success",
                                  "country": "Brazil",
                                  "countryCode": "BR",
                                  "region": "SP",
                                  "regionName": "São Paulo",
                                  "city": "São Paulo",
                                  "lat": -23.5505,
                                  "lon": -46.6333,
                                  "timezone": "America/Sao_Paulo",
                                  "isp": "VIVO",
                                  "query": "2804:14d:baa3:9a00::1"
                                }
                                """)));

        Optional<GeolocationInfo> result = adapter.findByIp(ip);

        assertThat(result).isPresent();
        assertThat(result.get().ip()).isEqualTo("2804:14d:baa3:9a00::1");
        assertThat(result.get().countryCode()).isEqualTo("BR");
        assertThat(result.get().city()).isEqualTo("São Paulo");
        assertThat(result.get().isp()).isEqualTo("VIVO");
    }
}
