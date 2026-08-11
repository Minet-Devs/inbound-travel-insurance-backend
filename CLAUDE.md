# Travel Insurance — Working Conventions

## Clarify before acting

When a task or prompt is unclear, ambiguous, or underspecified, ask a
clarifying question before starting work. Do not guess at intent or make
assumptions about scope, requirements, or approach when the request could
reasonably be interpreted more than one way — confirm first.

## Definition of done

Every code change must include, in the same piece of work:

1. **Tests** — mirror the existing test style:
   - Service logic → unit test in the matching `*ServiceImplTest` (Mockito, AssertJ).
   - New/changed endpoints → `@WebMvcTest` test in the matching `*ControllerTest`
     (note: `@WebMvcTest` uses default security, so non-GET requests need `.with(csrf())`
     and tests need `@WithMockUser`).
   - Run the affected tests before declaring the work done.
2. **Documentation** — update `backend-architecture.md` when adding or changing
   endpoints, entities, statuses/transitions, or cross-feature events.

## Build

- Requires JDK 21: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test`
- No `./mvnw` wrapper; use system `mvn`.

## Architecture notes

- Feature packages (`visitor`, `policy`, `benefit`, …); cross-feature references
  are ID columns only, and cross-feature calls go through the other feature's
  `*Service` interface, never its repository.
- Cross-feature side effects use in-process Spring events (e.g. `VisitorCreatedEvent`,
  `VisitorStatusChangedEvent`).
- `IllegalStateException` → 409, `ResourceNotFoundException` → 404
  (see `GlobalExceptionHandler`).
