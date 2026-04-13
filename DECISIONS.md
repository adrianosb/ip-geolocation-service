# Technical decisions

## Spring Boot

I picked Spring Boot because I'm comfortable with it. For a service this size the difference to other frameworks is negligible.

## Two layers: application + infrastructure

Two packages instead of three:

- `application/` -- business logic, models, port interfaces, exceptions. No framework imports.
- `infrastructure/` -- controllers, HTTP client, cache, validation, config. Depends on `application/`, never the other way around.

A dedicated `domain/` layer would just add folders without adding value here.

## Ports and adapters

Two port interfaces in `application/port/`:

- `GeolocationUseCase` (input) -- the controller depends on this, not on the service class directly.
- `GeolocationPort` (output) -- the service calls this to get data; the actual HTTP adapter lives in `infrastructure/external/`.

Dependencies always point inward: infrastructure -> application.

## Cache

The cache stores `GeolocationInfo` (raw API data) keyed by IP, not the full response. This way every cache hit gets a fresh timestamp and `source="cache"`. Fallback responses are never cached.

I didn't use `@Cacheable` because it would cache the whole `GeolocationResponse` including `source="api"`, and cache hits would look the same as fresh calls. Using `CacheManager` directly gives control over what goes in and what stays out.

Backend depends on the active Spring profile:

- Default: Caffeine in-memory. No external dependency needed.
- `redis` profile: Redis via `spring-boot-starter-data-redis`, serialized as JSON. Activated with `SPRING_PROFILES_ACTIVE=redis` and `REDIS_URL`.

`docker-compose.yml` sets the `redis` profile automatically. TTL and max size are configurable via `application.yaml` or environment variables.

## Records over Lombok

RNF5 says "Lombok or Records". Records are built into Java 17+ and cover everything needed here -- models, DTOs, config properties. No reason to add Lombok on top.

## Provider selection

The geolocation adapter is picked by `app.geolocation.provider` in `application.yaml` using `@ConditionalOnProperty`. To add a new provider: implement `GeolocationPort`, annotate it with the matching property value, change one line in the YAML.

## Mutation testing scope

Pitest runs only against `application.*`. Infrastructure code is already covered by integration and adapter tests. Running mutations there would mostly test framework wiring, not business logic.
