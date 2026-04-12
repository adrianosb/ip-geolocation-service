# Technical Decisions

## Two-layer architecture (application + infrastructure)

The project uses two layers instead of three:

- `application/`: business logic, models, port interfaces, exceptions. No framework dependencies.
- `infrastructure/`: controllers, external API client, cache, validation. Depends on `application/`, never the reverse.

A separate `domain/` layer would add complexity without benefit for a service this simple. Two layers are enough.

## Hexagonal ports and adapters

`GeolocationPort` is defined in `application/port/` and implemented in `infrastructure/external/`. The application layer defines what it needs; the infrastructure layer provides the implementation. This makes it easy to swap the external API client without touching business logic.

## Cache strategy: store GeolocationInfo, not GeolocationResponse

The cache stores `GeolocationInfo` (raw data from the API) keyed by IP. When serving a cache hit, a fresh `GeolocationResponse` is built with `source="cache"` and the current timestamp. This avoids returning a stale timestamp and keeps the `source` field accurate.

`@Cacheable` was not used here because it would cache the full `GeolocationResponse` with `source="api"`, making it impossible to distinguish cache hits on the response. Instead, the service interacts with `CacheManager` directly, which is more explicit and gives full control over what gets stored and when.

Fallback responses are never cached, as required by RF4 (only successful API responses are stored).

## Geolocation provider selection via configuration

The active geolocation adapter is selected by `app.geolocation.provider` in `application.yaml` using `@ConditionalOnProperty`. The current default is `ip-api`. To switch providers, create a new adapter implementing `GeolocationPort`, annotate it with the matching property value, and change one line in the YAML. No changes to the service or any other class are needed.

## Cache backend: Caffeine over Redis

The DESAVIO.md lists docker-compose with Redis as an optional differential. Redis was not added for a few reasons:

- The service runs as a single instance, so a distributed cache adds no benefit.
- The `GeolocationService` interacts with `CacheManager` directly (not via `@Cacheable`), which gives full control over `source` and timestamp accuracy. Migrating to Redis would require changing this approach and handling serialization for the `GeolocationInfo` record.
- Caffeine is already configured and working. The TTL and max-size are externalized in `application.yaml` and overridable via environment variables.

If the service ever needs to scale horizontally, replacing Caffeine with Redis means updating `CacheConfig` to use `RedisCacheManager` and adding `spring-boot-starter-data-redis`. The `GeolocationService` would need minor adjustments too, since it accesses the cache by casting to `Cache` from the Spring Cache abstraction.

## Mutation testing scope: application layer only

Pitest targets `com.adriano.ip_geolocation_service.application.*` (service, models, port, exceptions). The infrastructure layer is covered by integration and adapter tests, but mutation testing there would mostly exercise framework wiring rather than business logic. Limiting the scope keeps the mutation run fast (under 2 minutes) and focused on the code that matters most.
