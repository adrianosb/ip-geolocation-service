# Technical decisions

## Spring Boot

It's the framework I'm most familiar with, so I could focus on the problem itself.

## Two layers: application + infrastructure

I split the code into two packages:

- `application/` - business logic, models, port interfaces, exceptions. No framework imports.
- `infrastructure/` - controllers, HTTP client, cache, validation, config. Depends on `application/`, never the other way around.

A third `domain/` layer felt like overkill for a single-entity service. Two layers already keep the dependency direction clean.

## Ports and adapters

Two port interfaces in `application/port/`:

- `GeolocationUseCase` (input) - the controller calls this instead of the service class directly.
- `GeolocationPort` (output) - the service uses this to fetch data; the HTTP adapter that implements it lives in `infrastructure/external/`.

This keeps infrastructure depending on application, not the other way around.

## Cache

The cache stores `GeolocationInfo` (the raw API data) keyed by IP, not the full response. That way each cache hit still gets a fresh timestamp and `source="cache"`. Fallback responses are never cached.

I used `CacheManager` directly instead of `@Cacheable` because `@Cacheable` would store the entire `GeolocationResponse` including `source="api"`, and cache hits would look identical to fresh calls.

Backend depends on the active Spring profile:

- Default: Caffeine in-memory, no extra setup needed.
- `redis` profile: Redis via `spring-boot-starter-data-redis`, serialized as JSON. Activated with `SPRING_PROFILES_ACTIVE=redis` and `REDIS_URL`.

`docker-compose.yml` sets the `redis` profile automatically. TTL and max size are configurable in `application.yaml`.

## Records over Lombok

The requirements mention "Lombok or Records". Since Records are built into Java 17+ and cover what I needed here (models, DTOs, config properties), I went with them to avoid an extra dependency.

## Provider selection

The geolocation adapter is selected by `app.geolocation.provider` in `application.yaml` using `@ConditionalOnProperty`. Adding a new provider means implementing `GeolocationPort`, annotating it with the matching property value, and changing one YAML line.

## Mutation testing scope

Pitest targets `application.*` only. Infrastructure code is covered by integration and adapter tests, and running mutations there would mostly exercise framework wiring rather than business logic.
