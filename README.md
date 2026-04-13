# ip-geolocation-service

[![CI](https://github.com/adrianosb/ip-geolocation-service/actions/workflows/ci.yml/badge.svg)](https://github.com/adrianosb/ip-geolocation-service/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green)

REST microservice that receives an IP address and returns its geolocation: country, region, city, coordinates, timezone, and ISP. It calls ip-api.com under the hood, caches results with Caffeine (or Redis), and falls back to Brazil if the external API fails.

Live instance: https://ip-geolocation-service-production.up.railway.app

## Stack

- Java 21, Spring Boot 3.5, Maven
- Cache: Caffeine (local) / Redis (production)
- Tests: JUnit 5, Mockito, WireMock, Pitest
- Docs: SpringDoc OpenAPI (Swagger UI)

## Prerequisites

- Java 21
- Maven 3.9+

## Build and run

```bash
./mvnw install
./mvnw spring-boot:run
```

Starts on port 8080.

## Tests

```bash
./mvnw test
```

Mutation tests:

```bash
./mvnw test pitest:mutationCoverage
```

Report at `target/pit-reports/index.html`.

## API

`GET /api/geolocation/v1/locate`

- Query: `ip` (required) -- IPv4 or IPv6
- Header: `x-device-platform` (required) -- `iOS`, `Android`, or `Web`

### curl examples

```bash
# Public IP
curl "http://localhost:8080/api/geolocation/v1/locate?ip=8.8.8.8" \
  -H "x-device-platform: Web"

# Brazilian IP
curl "http://localhost:8080/api/geolocation/v1/locate?ip=177.45.123.45" \
  -H "x-device-platform: Android"

# Another Brazilian IP
curl "https://ip-geolocation-service-production.up.railway.app/api/geolocation/v1/locate?ip=187.95.109.100" \
  -H "x-device-platform: Web"

# IPv6
curl "http://localhost:8080/api/geolocation/v1/locate?ip=2001:4860:4860::8888" \
  -H "x-device-platform: iOS"

# Private IP -- returns Brazil fallback
curl "http://localhost:8080/api/geolocation/v1/locate?ip=192.168.1.1" \
  -H "x-device-platform: Web"

# Invalid IP -- returns 400
curl "http://localhost:8080/api/geolocation/v1/locate?ip=999.999.999.999" \
  -H "x-device-platform: Web"
```

Replace `localhost:8080` with `ip-geolocation-service-production.up.railway.app` to hit the live instance.

### Swagger UI

- Local: http://localhost:8080/swagger-ui.html
- Production: https://ip-geolocation-service-production.up.railway.app/swagger-ui.html

OpenAPI spec at `/v3/api-docs`.

## Docker

With Redis:

```bash
docker compose up
```

The `docker-compose.yml` activates the `redis` profile automatically.

Without Redis (Caffeine only):

```bash
docker build -t ip-geolocation-service .
docker run -p 8080:8080 ip-geolocation-service
```

## Postman collection

Import [ip-geolocation-service.postman_collection.json](ip-geolocation-service.postman_collection.json). The `{{baseUrl}}` variable defaults to `http://localhost:8080`.

## CI

GitHub Actions runs `mvn clean verify` on every push/PR to `main` or `develop`. Test reports are uploaded as artifacts.

## Technical decisions

| Decision | Choice | Why |
|---|---|---|
| Framework | Spring Boot | Familiarity; for this scope, differences with alternatives are minimal |
| Architecture | Two layers (`application/` + `infrastructure/`) | Clean/Hexagonal without a separate domain layer that would add no value here |
| Ports and adapters | Input port (`GeolocationUseCase`) + output port (`GeolocationPort`) | Dependency direction always points infrastructure toward application |
| Cache strategy | Caches raw `GeolocationInfo` by IP, not the full response | Cache hits get a fresh timestamp and correct `source="cache"`; fallbacks are never cached |
| Cache backend | Caffeine (local) / Redis (production, via Spring profile) | No external dependency for local dev; Redis for distributed production |
| Records over Lombok | Java Records | Native since Java 17, covers boilerplate reduction needed here without an extra dependency |
| Provider selection | `@ConditionalOnProperty` on `app.geolocation.provider` | Swap providers by changing one YAML property |
| Mutation testing | Scoped to `application.*` only | Keeps runs fast and focused on business logic |

Full rationale in [DECISIONS.md](DECISIONS.md).
