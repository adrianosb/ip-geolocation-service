package com.adriano.ip_geolocation_service.infrastructure.validation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class IpAddressValidatorTest {

    private final IpAddressValidator validator = new IpAddressValidator();

    @ParameterizedTest
    @ValueSource(strings = {
            "8.8.8.8",
            "177.45.123.45",
            "1.1.1.1",
            "203.0.113.1",
            "255.255.255.255"
    })
    void validPublicIpv4ShouldPassFormatCheck(String ip) {
        assertThat(validator.isValidFormat(ip)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "2001:4860:4860::8888",
            "2001:db8::1",
            "::1",
            "fe80::1",
            "2001:0db8:0000:0000:0000:0000:0000:0001",
            "2001:db8:85a3::8a2e:370:7334"
    })
    void validIpv6ShouldPassFormatCheck(String ip) {
        assertThat(validator.isValidFormat(ip)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "999.999.999.999",
            "256.0.0.1",
            "1.2.3",
            "1.2.3.4.5",
            "abc.def.ghi.jkl",
            "not-an-ip",
            "1.2.3.-1"
    })
    void invalidFormatShouldFailFormatCheck(String ip) {
        assertThat(validator.isValidFormat(ip)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "10.0.0.1, true",
            "10.255.255.255, true",
            "192.168.0.1, true",
            "192.168.1.100, true",
            "172.16.0.1, true",
            "172.31.255.255, true",
            "127.0.0.1, true",
            "127.0.0.100, true",
            "::1, true",
            "8.8.8.8, false",
            "1.1.1.1, false",
            "177.45.123.45, false",
            "172.15.0.1, false",
            "172.32.0.1, false"
    })
    void privateAndReservedIpsShouldBeIdentifiedCorrectly(String ip, boolean expected) {
        assertThat(validator.isPrivateOrReserved(ip)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = { "10.0.0.1", "192.168.0.1", "172.16.0.1", "127.0.0.1", "::1" })
    void privateIpsShouldHaveValidFormatButBeMarkedAsReserved(String ip) {
        assertThat(validator.isValidFormat(ip)).isTrue();
        assertThat(validator.isPrivateOrReserved(ip)).isTrue();
    }
}
