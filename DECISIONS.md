# Technical Decisions

## Two-layer architecture (application + infrastructure)

The project uses two layers instead of three:

- `application/` — business logic, models, port interfaces, exceptions. No framework dependencies.
- `infrastructure/` — controllers, external API client, cache, validation. Depends on `application/`, never the reverse.

A separate `domain/` layer would add complexity without benefit for a service this simple. Two layers are enough.

## Hexagonal ports and adapters

`GeolocationPort` is defined in `application/port/` and implemented in `infrastructure/external/`. The application layer defines what it needs; the infrastructure layer provides the implementation. This makes it easy to swap the external API client without touching business logic.
