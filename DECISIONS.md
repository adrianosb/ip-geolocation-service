# Technical Decisions

## Two-layer architecture (application + infrastructure)

The project uses two layers instead of three:

- `application/` — business logic, models, port interfaces, exceptions. No framework dependencies.
- `infrastructure/` — controllers, external API client, cache, validation. Depends on `application/`, never the reverse.

A separate `domain/` layer would add complexity without benefit for a service this simple. Two layers are enough.

## Hexagonal ports and adapters

`GeolocationPort` is defined in `application/port/` and implemented in `infrastructure/external/`. The application layer defines what it needs; the infrastructure layer provides the implementation. This makes it easy to swap the external API client without touching business logic.

## Cache strategy: store GeolocationInfo, not GeolocationResponse

The cache stores `GeolocationInfo` (raw data from the API) keyed by IP. When serving a cache hit, a fresh `GeolocationResponse` is built with `source="cache"` and the current timestamp. This avoids returning a stale timestamp and keeps the `source` field accurate.

`@Cacheable` was not used here because it would cache the full `GeolocationResponse` with `source="api"`, making it impossible to distinguish cache hits on the response. Instead, the service interacts with `CacheManager` directly, which is more explicit and gives full control over what gets stored and when.

Fallback responses are never cached, as required by RF4 (only successful API responses are stored).

## Geolocation provider selection via configuration

The active geolocation adapter is selected by `app.geolocation.provider` in `application.yaml` using `@ConditionalOnProperty`. The current default is `ip-api`. To switch providers, create a new adapter implementing `GeolocationPort`, annotate it with the matching property value, and change one line in the YAML. No changes to the service or any other class are needed.
