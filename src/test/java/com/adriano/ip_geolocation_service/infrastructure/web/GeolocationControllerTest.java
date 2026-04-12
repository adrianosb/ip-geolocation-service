package com.adriano.ip_geolocation_service.infrastructure.web;

import com.adriano.ip_geolocation_service.application.exception.InvalidIpAddressException;
import com.adriano.ip_geolocation_service.application.model.GeolocationResponse;
import com.adriano.ip_geolocation_service.application.service.GeolocationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GeolocationController.class)
class GeolocationControllerTest {

    private static final String URL = "/api/geolocation/v1/locate";
    private static final String PLATFORM_HEADER = "x-device-platform";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GeolocationService geolocationService;

    @Test
    void validRequest_returnsOkWithBody() throws Exception {
        GeolocationResponse response = buildResponse("8.8.8.8", "api");
        when(geolocationService.locate("8.8.8.8")).thenReturn(response);

        mockMvc.perform(get(URL).param("ip", "8.8.8.8").header(PLATFORM_HEADER, "Web"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ip").value("8.8.8.8"))
                .andExpect(jsonPath("$.country.code").value("US"))
                .andExpect(jsonPath("$.country.name").value("United States"))
                .andExpect(jsonPath("$.source").value("api"));
    }

    @Test
    void missingPlatformHeader_returns400() throws Exception {
        mockMvc.perform(get(URL).param("ip", "8.8.8.8"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_DEVICE_PLATFORM"));
    }

    @Test
    void invalidPlatformHeader_returns400() throws Exception {
        mockMvc.perform(get(URL).param("ip", "8.8.8.8").header(PLATFORM_HEADER, "Desktop"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_DEVICE_PLATFORM"));
    }

    @Test
    void invalidIp_returns400WithInvalidIpFormat() throws Exception {
        when(geolocationService.locate("not-an-ip")).thenThrow(new InvalidIpAddressException("not-an-ip"));

        mockMvc.perform(get(URL).param("ip", "not-an-ip").header(PLATFORM_HEADER, "Web"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_IP_FORMAT"))
                .andExpect(jsonPath("$.message").value("Invalid IP address format"));
    }

    @Test
    void missingIpParam_returns400() throws Exception {
        mockMvc.perform(get(URL).header(PLATFORM_HEADER, "Web"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MISSING_PARAMETER"));
    }

    @Test
    void fallbackResponse_returnsOkWithFallbackSource() throws Exception {
        GeolocationResponse response = buildFallbackResponse("10.0.0.1");
        when(geolocationService.locate("10.0.0.1")).thenReturn(response);

        mockMvc.perform(get(URL).param("ip", "10.0.0.1").header(PLATFORM_HEADER, "Android"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("fallback"))
                .andExpect(jsonPath("$.country.code").value("BR"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "desktop", "ANDROID", "ios", "WEB", "tablet", "" })
    void invalidPlatforms_return400(String platform) throws Exception {
        mockMvc.perform(get(URL).param("ip", "8.8.8.8").header(PLATFORM_HEADER, platform))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_DEVICE_PLATFORM"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "iOS", "Android", "Web" })
    void validPlatforms_return200(String platform) throws Exception {
        GeolocationResponse response = buildResponse("8.8.8.8", "api");
        when(geolocationService.locate("8.8.8.8")).thenReturn(response);

        mockMvc.perform(get(URL).param("ip", "8.8.8.8").header(PLATFORM_HEADER, platform))
                .andExpect(status().isOk());
    }

    private GeolocationResponse buildResponse(String ip, String source) {
        return new GeolocationResponse(
                ip,
                new GeolocationResponse.Country("US", "United States"),
                new GeolocationResponse.Region("CA", "California"),
                "Mountain View",
                new GeolocationResponse.Coordinates(37.3861, -122.0839),
                "America/Los_Angeles",
                "Google LLC",
                source,
                Instant.now());
    }

    private GeolocationResponse buildFallbackResponse(String ip) {
        return new GeolocationResponse(
                ip,
                new GeolocationResponse.Country("BR", "Brazil"),
                null,
                null,
                null,
                null,
                null,
                "fallback",
                Instant.now());
    }
}
