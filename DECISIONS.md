# Technical Decisions

## Framework: Spring Boot over Micronaut

The challenge allowed either Spring Boot or Micronaut. I chose Spring Boot because I know it well and am more comfortable working with it. Given the scope of this service, the practical difference between the two is minimal, so familiarity was the deciding factor.

## Two-layer architecture (application + infrastructure)

The challenge required a clear layered structure following Clean Architecture principles (RNF1). I also chose this approach because Clean Architecture and Hexagonal Architecture are patterns I have been studying, so this was a good opportunity to apply them in practice.

The project uses two layers instead of three:

- `application/`: business logic, models, port interfaces, exceptions. No framework dependencies.
- `infrastructure/`: controllers, external API client, cache, validation. Depends on `application/`, never the reverse.

A separate `domain/` layer would add complexity without benefit for a service this simple.

## Hexagonal ports and adapters

The application layer exposes two port interfaces in `application/port/`:

- **Input port** (`GeolocationUseCase`): defines the use case the controller depends on. The controller never references `GeolocationService` directly, only this interface.
- **Output port** (`GeolocationPort`): defines what the service needs from external data sources, implemented in `infrastructure/external/`.

This keeps the dependency direction consistent: `infrastructure → application` through interfaces on both sides, never the reverse.

## Cache

The cache stores `GeolocationInfo` (raw data from the API) keyed by IP, not the full `GeolocationResponse`. This way, a cache hit always produces a fresh timestamp and accurate `source="cache"` on the response. Fallback responses are not cached, as required by RF4.

`@Cacheable` was not used because it would cache the complete `GeolocationResponse` with `source="api"`, making cache hits indistinguishable on the response. Instead, the service accesses `CacheManager` directly, which gives full control over what gets stored and when.

The cache backend is selected by Spring profile:

- **Default (local)**: Caffeine in-memory cache. No external dependency, works out of the box, configured with TTL and max size from `application.yaml`.
- **`redis` profile (production)**: Redis via `spring-boot-starter-data-redis`. Activated by setting `SPRING_PROFILES_ACTIVE=redis` and `REDIS_URL`. Values are serialized as JSON using `GenericJackson2JsonRedisSerializer`. On Railway, `REDIS_URL` maps to the Redis plugin's connection URL.

`docker-compose.yml` runs both services and sets the `redis` profile automatically. TTL and max size remain externalized and overridable via environment variables.

## Java Records over Lombok

The challenge stack listed Lombok, and RNF5 says "Lombok or Records". I chose Records because they are a native Java 17+ feature that covers all the boilerplate reduction needed here (models, DTOs, config properties) without an extra dependency. Lombok would add nothing that Records do not already provide for this project.

## Geolocation provider selection via configuration

The active geolocation adapter is selected by `app.geolocation.provider` in `application.yaml` using `@ConditionalOnProperty`. The current default is `ip-api`. To switch providers, create a new adapter implementing `GeolocationPort`, annotate it with the matching property value, and change one line in the YAML. No changes to the service or any other class are needed.

## Mutation testing scope: application layer only

Pitest targets `com.adriano.ip_geolocation_service.application.*`. The infrastructure layer is covered by integration and adapter tests, but mutation testing there would mostly exercise framework wiring rather than business logic. Limiting the scope keeps the mutation run fast and focused on the code that matters most.
