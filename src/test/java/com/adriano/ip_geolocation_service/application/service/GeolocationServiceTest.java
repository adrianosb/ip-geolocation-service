package com.adriano.ip_geolocation_service.application.service;

import com.adriano.ip_geolocation_service.application.exception.InvalidIpAddressException;
import com.adriano.ip_geolocation_service.application.model.FallbackCountry;
import com.adriano.ip_geolocation_service.application.model.GeolocationInfo;
import com.adriano.ip_geolocation_service.application.model.GeolocationResponse;
import com.adriano.ip_geolocation_service.application.port.GeolocationCachePort;
import com.adriano.ip_geolocation_service.application.port.GeolocationPort;
import com.adriano.ip_geolocation_service.application.port.IpValidationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeolocationServiceTest {

    @Mock
    private GeolocationPort geolocationPort;

    @Mock
    private IpValidationPort validator;

    @Mock
    private GeolocationCachePort cache;

    private GeolocationService service;

    @BeforeEach
    void setUp() {
        service = new GeolocationService(geolocationPort, validator, cache,
                new FallbackCountry("BR", "Brazil"));
    }

    @Test
    void locate_validIp_apiReturnsData_returnsResponseWithSourceApi() {
        String ip = "8.8.8.8";
        GeolocationInfo info = new GeolocationInfo(ip, "US", "United States", "CA", "California",
                "Mountain View", 37.386, -122.0838, "America/Los_Angeles", "Google LLC");

        when(validator.isValidFormat(ip)).thenReturn(true);
        when(validator.isPrivateOrReserved(ip)).thenReturn(false);
        when(cache.get(ip)).thenReturn(Optional.empty());
        when(geolocationPort.findByIp(ip)).thenReturn(Optional.of(info));

        GeolocationResponse response = service.locate(ip);

        assertThat(response.ip()).isEqualTo(ip);
        assertThat(response.source()).isEqualTo("api");
        assertThat(response.country().code()).isEqualTo("US");
        assertThat(response.country().name()).isEqualTo("United States");
        assertThat(response.region().code()).isEqualTo("CA");
        assertThat(response.city()).isEqualTo("Mountain View");
        assertThat(response.timestamp()).isNotNull();

        verify(cache).put(ip, info);
    }

    @Test
    void locate_cacheHit_doesNotCallPort() {
        String ip = "8.8.8.8";
        GeolocationInfo cached = new GeolocationInfo(ip, "US", "United States", "CA", "California",
                "Mountain View", 37.386, -122.0838, "America/Los_Angeles", "Google LLC");

        when(validator.isValidFormat(ip)).thenReturn(true);
        when(validator.isPrivateOrReserved(ip)).thenReturn(false);
        when(cache.get(ip)).thenReturn(Optional.of(cached));

        GeolocationResponse response = service.locate(ip);

        assertThat(response.source()).isEqualTo("cache");
        assertThat(response.ip()).isEqualTo(ip);
        verifyNoInteractions(geolocationPort);
    }

    @Test
    void locate_privateIp_returnsFallbackBrazil() {
        String ip = "192.168.1.1";

        when(validator.isValidFormat(ip)).thenReturn(true);
        when(validator.isPrivateOrReserved(ip)).thenReturn(true);

        GeolocationResponse response = service.locate(ip);

        assertThat(response.ip()).isEqualTo(ip);
        assertThat(response.source()).isEqualTo("fallback");
        assertThat(response.country().code()).isEqualTo("BR");
        assertThat(response.country().name()).isEqualTo("Brazil");
        assertThat(response.region()).isNull();
        verifyNoInteractions(geolocationPort, cache);
    }

    @Test
    void locate_localhostIp_returnsFallbackBrazil() {
        String ip = "127.0.0.1";

        when(validator.isValidFormat(ip)).thenReturn(true);
        when(validator.isPrivateOrReserved(ip)).thenReturn(true);

        GeolocationResponse response = service.locate(ip);

        assertThat(response.ip()).isEqualTo(ip);
        assertThat(response.source()).isEqualTo("fallback");
        assertThat(response.country().code()).isEqualTo("BR");
        verifyNoInteractions(geolocationPort, cache);
    }

    @Test
    @SuppressWarnings("null")
    void locate_apiReturnsEmpty_returnsFallbackBrazil() {
        String ip = "8.8.8.8";

        when(validator.isValidFormat(ip)).thenReturn(true);
        when(validator.isPrivateOrReserved(ip)).thenReturn(false);
        when(cache.get(ip)).thenReturn(Optional.empty());
        when(geolocationPort.findByIp(ip)).thenReturn(Optional.empty());

        GeolocationResponse response = service.locate(ip);

        assertThat(response.source()).isEqualTo("fallback");
        assertThat(response.country().code()).isEqualTo("BR");
        assertThat(response.country().name()).isEqualTo("Brazil");
        verify(cache, never()).put(any(String.class), any(GeolocationInfo.class));
    }

    @Test
    void locate_invalidIp_throwsInvalidIpAddressException() {
        String ip = "not-an-ip";

        when(validator.isValidFormat(ip)).thenReturn(false);

        assertThatThrownBy(() -> service.locate(ip))
                .isInstanceOf(InvalidIpAddressException.class);

        verifyNoInteractions(geolocationPort, cache);
    }

    @Test
    void locate_cacheMiss_callsPort() {
        String ip = "8.8.8.8";
        GeolocationInfo info = new GeolocationInfo(ip, "US", "United States", "CA", "California",
                "Mountain View", 37.386, -122.0838, "America/Los_Angeles", "Google LLC");

        when(validator.isValidFormat(ip)).thenReturn(true);
        when(validator.isPrivateOrReserved(ip)).thenReturn(false);
        when(cache.get(ip)).thenReturn(Optional.empty());
        when(geolocationPort.findByIp(ip)).thenReturn(Optional.of(info));

        GeolocationResponse response = service.locate(ip);

        assertThat(response.source()).isEqualTo("api");
    }
}
