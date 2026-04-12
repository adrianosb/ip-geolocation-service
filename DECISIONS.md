# Technical Decisions

## Two-layer architecture (application + infrastructure)

The project uses two layers instead of three:

- `application/`: business logic, models, port interfaces, exceptions. No framework dependencies.
- `infrastructure/`: controllers, external API client, cache, validation. Depends on `application/`, never the reverse.

A separate `domain/` layer would add complexity without benefit for a service this simple.

## Hexagonal ports and adapters

`GeolocationPort` is defined in `application/port/` and implemented in `infrastructure/external/`. The application layer defines what it needs; the infrastructure layer provides the implementation. This makes it easy to swap the external API client without touching business logic.

## Cache

The cache stores `GeolocationInfo` (raw data from the API) keyed by IP, not the full `GeolocationResponse`. This way, a cache hit always produces a fresh timestamp and accurate `source="cache"` on the response. Fallback responses are not cached, as required by RF4.

`@Cacheable` was not used because it would cache the complete `GeolocationResponse` with `source="api"`, making cache hits indistinguishable on the response. Instead, the service accesses `CacheManager` directly, which gives full control over what gets stored and when.

Caffeine was chosen over Redis. The service runs as a single instance, so a distributed cache adds no benefit. TTL and max size are externalized in `application.yaml` and overridable via environment variables. If the service ever needs to scale horizontally, the change is limited to `CacheConfig` (switch to `RedisCacheManager`) and minor adjustments in `GeolocationService`.

## Geolocation provider selection via configuration

The active geolocation adapter is selected by `app.geolocation.provider` in `application.yaml` using `@ConditionalOnProperty`. The current default is `ip-api`. To switch providers, create a new adapter implementing `GeolocationPort`, annotate it with the matching property value, and change one line in the YAML. No changes to the service or any other class are needed.

## Mutation testing scope: application layer only

Pitest targets `com.adriano.ip_geolocation_service.application.*`. The infrastructure layer is covered by integration and adapter tests, but mutation testing there would mostly exercise framework wiring rather than business logic. Limiting the scope keeps the mutation run fast and focused on the code that matters most.
