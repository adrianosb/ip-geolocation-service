package com.adriano.ip_geolocation_service.infrastructure.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
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

    @ParameterizedTest
    @ValueSource(strings = {
            "::", // unspecified address (0:0:0:0:0:0:0:0) — both halves empty after split
            "1::", // trailing double colon, right half empty
            "::ffff:0:0", // left half empty, right has groups
            "fe80::", // link-local prefix, right half empty
    })
    void ipv6WithDoubleColonEdgeCasesShouldPassFormatCheck(String ip) {
        assertThat(validator.isValidFormat(ip)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            ":::", // triple colon
            "1::2::3", // two double colons
            "12345::1", // hex group with 5 chars
            "gggg::1", // invalid hex chars
            "::0:0:0:0:0:0:0:0" // eight explicit groups plus :: = too many
    })
    void invalidIpv6PatternsShouldFailFormatCheck(String ip) {
        assertThat(validator.isValidFormat(ip)).isFalse();
    }

    @Test
    void unspecifiedAddressShouldBeValidButNotReserved() {
        assertThat(validator.isValidFormat("::")).isTrue();
        assertThat(validator.isPrivateOrReserved("::")).isFalse();
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @ValueSource(strings = { "   " })
    void nullOrBlankShouldFailFormatCheck(String ip) {
        assertThat(validator.isValidFormat(ip)).isFalse();
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @ValueSource(strings = { "   " })
    void nullOrBlankShouldNotBeConsideredPrivateOrReserved(String ip) {
        assertThat(validator.isPrivateOrReserved(ip)).isFalse();
    }
}
