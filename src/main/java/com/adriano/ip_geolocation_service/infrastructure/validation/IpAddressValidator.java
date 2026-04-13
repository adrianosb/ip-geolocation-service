package com.adriano.ip_geolocation_service.infrastructure.validation;

import com.adriano.ip_geolocation_service.application.port.IpValidationPort;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class IpAddressValidator implements IpValidationPort {

    private static final Pattern IPV4_FORMAT = Pattern.compile(
            "^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");

    private static final Pattern HEX_GROUP = Pattern.compile(
            "[0-9a-fA-F]{1,4}");

    private static final Pattern PRIVATE_10 = Pattern.compile(
            "^10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");

    private static final Pattern PRIVATE_192_168 = Pattern.compile(
            "^192\\.168\\.\\d{1,3}\\.\\d{1,3}$");

    private static final Pattern PRIVATE_172 = Pattern.compile(
            "^172\\.(1[6-9]|2\\d|3[01])\\.\\d{1,3}\\.\\d{1,3}$");

    private static final Pattern PRIVATE_127 = Pattern.compile(
            "^127\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");

    /**
     * Returns true if the IP has a valid format (IPv4 or IPv6).
     */
    public boolean isValidFormat(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        return isValidIpv4(ip) || isValidIpv6(ip);
    }

    /**
     * Returns true if the IP is private, reserved, or localhost.
     * These IPs return a fallback response instead of a 400 error.
     */
    public boolean isPrivateOrReserved(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        return PRIVATE_10.matcher(ip).matches()
                || PRIVATE_192_168.matcher(ip).matches()
                || PRIVATE_172.matcher(ip).matches()
                || PRIVATE_127.matcher(ip).matches()
                || "::1".equals(ip);
    }

    private boolean isValidIpv4(String ip) {
        var matcher = IPV4_FORMAT.matcher(ip);
        if (!matcher.matches()) {
            return false;
        }
        for (int i = 1; i <= 4; i++) {
            if (Integer.parseInt(matcher.group(i)) > 255) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidIpv6(String ip) {
        if (ip.contains(":::")) {
            return false;
        }
        String[] halves = ip.split("::", -1);
        if (halves.length > 2) {
            return false;
        }
        if (halves.length == 2) {
            int left = countValidHexGroups(halves[0]);
            int right = countValidHexGroups(halves[1]);
            if (left < 0 || right < 0) {
                return false;
            }
            return (left + right) <= 7;
        }
        return countValidHexGroups(ip) == 8;
    }

    private int countValidHexGroups(String part) {
        if (part.isEmpty()) {
            return 0;
        }
        String[] groups = part.split(":", -1);
        for (String group : groups) {
            if (!HEX_GROUP.matcher(group).matches()) {
                return -1;
            }
        }
        return groups.length;
    }
}
