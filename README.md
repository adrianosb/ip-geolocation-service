# ip-geolocation-service

[![CI](https://github.com/adrianosb/ip-geolocation-service/actions/workflows/ci.yml/badge.svg)](https://github.com/adrianosb/ip-geolocation-service/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green)

A REST service that takes an IP address and returns geographic information about it: country, region, city, coordinates, timezone, and ISP.

## What it does

- Accepts a GET request with an IP parameter and a required `x-device-platform` header
- Calls ip-api.com to fetch geolocation data
- Caches results for 24 hours (configurable). Locally uses Caffeine (in-memory); in production uses Redis (distributed)
- Falls back to Brazil as default country if the external API fails or the IP is private/reserved
- Validates IP format and rejects invalid or private addresses with clear error messages

## Stack

- Java 21 (Temurin)
- Spring Boot 3.5.x
- Spring Cache + Caffeine (local) / Redis (production)

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

To run mutation tests:

```bash
./mvnw test pitest:mutationCoverage
```

The HTML report is generated at `target/pit-reports/index.html`.

## API

### Endpoint

`GET /api/geolocation/v1/locate`

**Query parameters:**
- `ip` (required) - IPv4 or IPv6 address

**Headers:**
- `x-device-platform` (required) - one of: `iOS`, `Android`, `Web`

### Examples

Using the live service:

```bash
# Public IP (Google DNS)
curl "https://ip-geolocation-service-production.up.railway.app/api/geolocation/v1/locate?ip=8.8.8.8" \
  -H "x-device-platform: Web"

# Brazilian IP
curl "https://ip-geolocation-service-production.up.railway.app/api/geolocation/v1/locate?ip=177.45.123.45" \
  -H "x-device-platform: Android"

# IPv6
curl "https://ip-geolocation-service-production.up.railway.app/api/geolocation/v1/locate?ip=2001:4860:4860::8888" \
  -H "x-device-platform: iOS"

# Private IP (returns Brazil fallback)
curl "https://ip-geolocation-service-production.up.railway.app/api/geolocation/v1/locate?ip=192.168.1.1" \
  -H "x-device-platform: Web"

# Invalid IP (returns 400)
curl "https://ip-geolocation-service-production.up.railway.app/api/geolocation/v1/locate?ip=999.999.999.999" \
  -H "x-device-platform: Web"
```

Or against a local instance replacing the host with `http://localhost:8080`.

### Interactive documentation

Swagger UI is available at:
- **Production:** https://ip-geolocation-service-production.up.railway.app/swagger-ui.html
- **Local:** http://localhost:8080/swagger-ui.html

The OpenAPI spec is at `/v3/api-docs`.

## Docker

Runs the application together with a Redis instance:

```bash
docker compose up
```

The `redis` Spring profile is activated automatically by `docker-compose.yml`. This is the same setup used in production.

Or build and run without Redis (Caffeine cache):

```bash
docker build -t ip-geolocation-service .
docker run -p 8080:8080 ip-geolocation-service
```

## Postman/Insomnia Collection

A ready-to-use collection is available at [ip-geolocation-service.postman_collection.json](ip-geolocation-service.postman_collection.json).

The base URL is set as a `{{baseUrl}}` variable (default: `http://localhost:8080`), change it once to point to any environment.

## CI

The project uses GitHub Actions for continuous integration. On every push or pull request to `main` or `develop`, it runs `mvn clean verify` which compiles, runs all tests, and verifies the build. Test reports are uploaded as artifacts and kept for 7 days.
