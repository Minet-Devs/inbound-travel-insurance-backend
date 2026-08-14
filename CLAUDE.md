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

## Database migrations

- Flyway files use pure-timestamp versioning:
  `V<yyyyMMddHHmm>__<description>.sql` (e.g. `V202608131430__add_claim_status.sql`).
  No sequential number prefix — see `backend-architecture.md` for why.
- When creating a new migration, get the real current timestamp with
  `date +%Y%m%d%H%M` rather than guessing or reusing a value from an example.
- Never rename existing migrations (`V001` … `V032`); the convention applies
  only to new files going forward.

## Architecture notes

- Feature packages (`visitor`, `policy`, `benefit`, …); cross-feature references
  are ID columns only, and cross-feature calls go through the other feature's
  `*Service` interface, never its repository.
- Cross-feature side effects use in-process Spring events (e.g. `VisitorCreatedEvent`,
  `VisitorStatusChangedEvent`).
- `IllegalStateException` → 409, `ResourceNotFoundException` → 404
  (see `GlobalExceptionHandler`).
