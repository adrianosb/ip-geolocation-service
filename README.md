# ip-geolocation-service

A REST service that takes an IP address and returns geographic information about it: country, region, city, coordinates, timezone, and ISP.

## What it does

- Accepts a GET request with an IP parameter and a required `x-device-platform` header
- Calls ip-api.com to fetch geolocation data
- Caches results in memory for 24 hours (configurable) to avoid repeated external calls
- Falls back to Brazil as default country if the external API fails or the IP is private/reserved
- Validates IP format and rejects invalid or private addresses with clear error messages

## Stack

- Java 21 (Temurin)
- Spring Boot 3.5.x
- Spring Cache + Caffeine (in-memory cache)
- java.net.http.HttpClient (no RestTemplate or WebClient)
- Lombok
- SpringDoc OpenAPI (Swagger UI)
- JUnit 5, AssertJ, Mockito, WireMock
- Maven

## Prerequisites

- Java 21
- Maven 3.9+

## Install

```bash
./mvnw install
```

## Running

```bash
./mvnw spring-boot:run
```

The service starts on port 8080 by default.

## Tests

```bash
./mvnw test
```

## API

### Endpoint

`GET /api/geolocation/v1/locate`

**Query parameters:**
- `ip` (required) - IPv4 or IPv6 address

**Headers:**
- `x-device-platform` (required) - one of: `iOS`, `Android`, `Web`

### Examples

```bash
# Public IP
curl "http://localhost:8080/api/geolocation/v1/locate?ip=8.8.8.8" \
  -H "x-device-platform: Web"

# Brazilian IP
curl "http://localhost:8080/api/geolocation/v1/locate?ip=177.45.123.45" \
  -H "x-device-platform: Android"

# IPv6
curl "http://localhost:8080/api/geolocation/v1/locate?ip=2001:4860:4860::8888" \
  -H "x-device-platform: iOS"

# Private IP (returns Brazil fallback)
curl "http://localhost:8080/api/geolocation/v1/locate?ip=192.168.1.1" \
  -H "x-device-platform: Web"

# Invalid IP (returns 400)
curl "http://localhost:8080/api/geolocation/v1/locate?ip=999.999.999.999" \
  -H "x-device-platform: Web"
```

### Interactive documentation

Swagger UI is available at `http://localhost:8080/swagger-ui.html` when the service is running. The OpenAPI spec is at `http://localhost:8080/v3/api-docs`.

## Docker

Build and run with a single command:

```bash
docker compose up
```

Or build and run manually:

```bash
docker build -t ip-geolocation-service .
docker run -p 8080:8080 ip-geolocation-service
```

The service is available at `http://localhost:8080` in both cases.

## CI

The project uses GitHub Actions for continuous integration. On every push or pull request to `main` or `develop`, it runs `mvn clean verify` which compiles, runs all tests, and verifies the build. Test reports are uploaded as artifacts and kept for 7 days.
