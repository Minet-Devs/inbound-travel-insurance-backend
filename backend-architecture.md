# Travel Insurance — Backend Architecture

This document describes the architecture of the Inbound Travel Medical Insurance backend.
It is the reference for how the codebase is organized, how the domain model fits
together, and the conventions every contribution is expected to follow. Read it
before writing your first feature.

The `Policy`, `Benefit`, and `Visitor` shapes described below reflect the
requirements of the Ministry of Health's Mandatory Inbound Travel Health
Insurance framework — see `travel-insurance.md` for the underlying regulatory
summary and the full requirement-vs-codebase gap analysis.

## Contents

- [Travel Insurance — Backend Architecture](#travel-insurance--backend-architecture)
  - [Contents](#contents)
  - [Tech Stack](#tech-stack)
  - [Project Layout (Package by Feature)](#project-layout-package-by-feature)
  - [Core Insurance Flow](#core-insurance-flow)
  - [Users, Roles \& Organizations](#users-roles--organizations)
  - [Layering Rules](#layering-rules)
  - [Base Entity, Auditing \& Soft Delete](#base-entity-auditing--soft-delete)
  - [Conventions](#conventions)
  - [REST Resources](#rest-resources)
  - [Messaging (RabbitMQ)](#messaging-rabbitmq)
  - [Notifications (Policy Document Email)](#notifications-policy-document-email)
  - [API Documentation (Swagger)](#api-documentation-swagger)
  - [Security](#security)
  - [Database Connection Pool (HikariCP)](#database-connection-pool-hikaricp)
  - [Code Practices](#code-practices)
  - [Maven Dependencies](#maven-dependencies)

## Tech Stack

- Java 21
- Spring Boot 3.x (Web, Data JPA, Validation, Security)
- PostgreSQL (runtime) / H2 (tests)
- RabbitMQ (messaging)
- SMTP + Thymeleaf + openhtmltopdf (policy document email on visitor activation)
- Flyway (database migrations)
- Maven

## Project Layout (Package by Feature)

The codebase is organized by **feature**, not by technical layer. Each feature
package contains its own controller, service (interface + implementation),
repository, domain entity, mapper, and DTOs.

```
com.travel.insurance/
│
├── 📁 config/                              # Global configuration
│   ├── SecurityConfig.java                 # Filter chain, route rules, PasswordEncoder
│   ├── JpaAuditingConfig.java              # @EnableJpaAuditing + AuditorAware
│   ├── OpenApiConfig.java                  # Swagger/OpenAPI metadata
│   ├── RabbitConfig.java                   # Exchanges, queues, bindings
│   └── MailProperties.java                 # app.mail.* (from address, emergency-assistance contact)
│
├── 📁 common/                              # Shared, feature-agnostic code
│   ├── 📁 domain/
│   │   └── BaseEntity.java                 # @MappedSuperclass: ID, audit + soft-delete fields
│   ├── 📁 exception/
│   │   ├── GlobalExceptionHandler.java     # @RestControllerAdvice
│   │   ├── ResourceNotFoundException.java
│   │   └── ApiError.java                   # Standard error response body
│   ├── 📁 messaging/
│   │   └── EventPublisher.java             # Thin wrapper over RabbitTemplate
│   ├── 📁 email/
│   │   └── EmailService.java               # Thin wrapper over JavaMailSender
│   └── 📁 util/
│
├── 📁 notification/                        # Feature: Visitor-facing notifications
│   ├── VisitorActivatedNotificationListener.java  # @TransactionalEventListener(AFTER_COMMIT)
│   │                                       # on VisitorStatusChangedEvent; composes
│   │                                       # Visitor+Policy+VisitorBenefit+Insurer data
│   ├── PolicyDocumentRenderer.java         # Thymeleaf → HTML → PDF (openhtmltopdf)
│   └── PolicyDocumentData.java             # Internal template data holder (not a DTO)
│
├── 📁 auth/                                # Feature: Authentication
│   ├── AuthController.java                 # /login, /refresh
│   ├── AuthService.java                    # Service interface
│   ├── AuthServiceImpl.java                # Credential checks, token issuing
│   ├── JwtTokenProvider.java               # Token creation and validation
│   └── 📁 dto/
│       ├── LoginRequest.java
│       └── TokenResponse.java
│
├── 📁 user/                                # Feature: User Management
│   ├── UserController.java                 # Web layer (@RestController)
│   ├── UserService.java                    # Service interface (business contract)
│   ├── UserServiceImpl.java                # @Service implementation
│   ├── UserRepository.java                 # Data access (extends JpaRepository)
│   ├── User.java                           # Domain entity (@Entity)
│   ├── Role.java                           # Enum: ADMIN, INSURER_USER,
│   │                                       #       PROVIDER_USER
│   ├── UserMapper.java                     # Entity ⇄ DTO mapping
│   └── 📁 dto/
│       ├── UserRequest.java
│       └── UserResponse.java
│
├── 📁 insurer/                             # Feature: Insurer Management
│   ├── InsurerController.java
│   ├── InsurerService.java                 # Interface
│   ├── InsurerServiceImpl.java
│   ├── InsurerRepository.java
│   ├── Insurer.java
│   ├── InsurerMapper.java
│   └── 📁 dto/
│       ├── InsurerRequest.java
│       └── InsurerResponse.java
│
├── 📁 serviceprovider/                     # Feature: Service Provider Management
│   ├── ServiceProviderController.java
│   ├── ServiceProviderService.java         # Interface
│   ├── ServiceProviderServiceImpl.java
│   ├── ServiceProviderRepository.java
│   ├── ServiceProvider.java
│   ├── ServiceProviderMapper.java
│   └── 📁 dto/
│       ├── ServiceProviderRequest.java
│       └── ServiceProviderResponse.java
│
├── 📁 policy/                              # Feature: Policy Management
│   ├── PolicyController.java
│   ├── PolicyService.java                  # Interface
│   ├── PolicyServiceImpl.java
│   ├── PolicyRepository.java
│   ├── Policy.java                         # insurerIds (set), policyType
│   ├── PolicyStatus.java                   # Enum: DRAFT, ACTIVE, EXPIRED, CANCELLED
│   ├── PolicyType.java                     # Enum: SINGLE_ENTRY_UP_TO_30_DAYS,
│   │                                       #       SINGLE_ENTRY_31_TO_60_DAYS,
│   │                                       #       IPMI_61_DAYS_TO_12_MONTHS
│   ├── PolicyMapper.java
│   └── 📁 dto/
│       ├── PolicyRequest.java
│       ├── PolicyResponse.java
│       └── PolicyDetailResponse.java       # PolicyResponse + embedded global benefit catalog
│
├── 📁 benefit/                             # Feature: Benefit Catalog (global)
│   ├── BenefitController.java
│   ├── BenefitService.java                 # Interface
│   ├── BenefitServiceImpl.java
│   ├── BenefitRepository.java
│   ├── Benefit.java                        # benefitName, limitAmount (no policy link)
│   ├── BenefitMapper.java
│   └── 📁 dto/
│       ├── BenefitRequest.java
│       └── BenefitResponse.java
│
├── 📁 visitor/                             # Feature: Visitor (insured traveler) Management
│   ├── VisitorController.java
│   ├── VisitorService.java                 # Interface
│   ├── VisitorServiceImpl.java
│   ├── VisitorRepository.java
│   ├── Visitor.java                        # policyId + passport-based KYC attributes,
│   │                                       # incl. address, facePhotoUrl, reasonForTravel,
│   │                                       # underlyingConditions
│   ├── VisitorCreatedEvent.java            # In-process event on visitor creation;
│   │                                       # consumed by visitorbenefit to seed benefits
│   ├── Gender.java                         # Enum: MALE, FEMALE, OTHER
│   ├── MaritalStatus.java                  # Enum: SINGLE, MARRIED, DIVORCED, WIDOWED
│   ├── VisitorMapper.java
│   └── 📁 dto/
│       ├── VisitorRequest.java
│       ├── VisitorResponse.java
│       └── VisitorDetailResponse.java      # KYC + assigned visitor benefits
│
├── 📁 visitorbenefit/                      # Feature: Benefits assigned to a visitor
│   ├── VisitorBenefitController.java
│   ├── VisitorBenefitService.java          # Interface
│   ├── VisitorBenefitServiceImpl.java
│   ├── VisitorBenefitRepository.java
│   ├── VisitorBenefit.java                 # visitorId, benefitId, limitAmount
│   ├── VisitorCreatedListener.java         # seeds visitor benefits from the
│   │                                       # catalog on VisitorCreatedEvent
│   ├── VisitorBenefitMapper.java
│   └── 📁 dto/
│       ├── VisitorBenefitRequest.java
│       └── VisitorBenefitResponse.java
│
├── 📁 preauthorization/                    # Feature: Pre-authorization Requests
│   ├── PreauthorizationController.java
│   ├── PreauthorizationService.java        # Interface
│   ├── PreauthorizationServiceImpl.java
│   ├── PreauthorizationRepository.java
│   ├── Preauthorization.java               # policyId, benefitId, serviceProviderId,
│   │                                       # requestedAmount, approvedAmount
│   ├── PreauthorizationStatus.java         # Enum: PENDING, APPROVED, PARTIALLY_APPROVED,
│   │                                       #       REJECTED, EXPIRED
│   ├── PreauthorizationMapper.java
│   └── 📁 dto/
│       ├── PreauthorizationRequest.java
│       ├── PreauthorizationDecisionRequest.java   # Approve/reject with amount and reason
│       └── PreauthorizationResponse.java
│
├── 📁 claim/                               # Feature: Claims Processing
│   ├── ClaimController.java
│   ├── ClaimService.java                   # Interface
│   ├── ClaimServiceImpl.java
│   ├── ClaimRepository.java
│   ├── Claim.java                          # policyId, benefitId, serviceProviderId,
│   │                                       # preauthorizationId (nullable), claimedAmount,
│   │                                       # approvedAmount
│   ├── ClaimStatus.java                    # Enum: SUBMITTED, UNDER_REVIEW, APPROVED,
│   │                                       #       PARTIALLY_APPROVED, REJECTED, PAID
│   ├── ClaimMapper.java
│   └── 📁 dto/
│       ├── ClaimRequest.java
│       ├── ClaimDecisionRequest.java
│       └── ClaimResponse.java
│
└── TravelInsuranceApplication.java         # @SpringBootApplication entry point
```

## Core Insurance Flow

```
Benefit                                   (a global catalog of named benefits with limits,
                                           not scoped to any policy)

Policy
   │
   ├──1:N── Visitor ──1:N── VisitorBenefit  (the insured travelers and the benefits
   │            (KYC record;   assigned to them; each row references a global
   │             holds policyId) Benefit and snapshots its own limitAmount)
   │
   ├──1:N── Preauthorization ──0:1── Claim
   │            (provider asks for approval  (a claim may reference the
   │             before rendering a service)  pre-authorization that authorized it)
   └──1:N── Claim                          (claims may also arrive without a
                                            pre-authorization, e.g. reimbursement
                                            of out-of-pocket costs)
```

- A **Policy** is the insurance contract. It references a set of backing
  insurers (`insurerIds`) and carries a `policyType` and a status, but no
  cover dates of its own — one policy covers many visitors, each entering
  and leaving on their own schedule, so a fixed date range doesn't belong at
  the policy level. `policyType` is one of the three cover periods mandated
  by the Ministry of Health's Mandatory Inbound Travel Health Insurance
  framework (`PolicyType`: `SINGLE_ENTRY_UP_TO_30_DAYS`,
  `SINGLE_ENTRY_31_TO_60_DAYS`, `IPMI_61_DAYS_TO_12_MONTHS`), each carrying a
  min/max day range; it's enforced per visitor instead (see below). A policy
  holds no treatment-level detail. `GET /api/v1/policies/{id}` and the paged
  `GET /api/v1/policies` return `PolicyDetailResponse` rows that embed the
  benefit catalog under `benefits`; since benefits are global (see below),
  every policy carries the whole catalog. `PolicyController` fetches it once
  via `BenefitService.listAll()` and attaches it to each policy. Create/update
  return plain `PolicyResponse` rows without benefits.
- **Benefit** is a standalone **global catalog** entry: a `benefitName` (free
  text) and a `limitAmount` (limit of cover). It is no longer scoped to a
  policy — there is no `policyId` or fixed `BenefitType` enum. The catalog is
  managed directly through full CRUD (`POST/GET/PUT/DELETE /api/v1/benefits`);
  names are not required to be unique. Consumption is not tracked against the
  limit. Because there is no policy link, a policy's `benefits` in
  `PolicyDetailResponse` is simply the entire catalog. Other features
  reference a benefit by ID only: `VisitorBenefit`, `Preauthorization` and
  `Claim` validate that the referenced benefit exists (via
  `BenefitService.getEntityById`), but no longer that it belongs to a
  particular policy.
- A **Visitor** is an insured traveler behind a policy. It carries a
  `policyId` (ID-only reference — one policy may cover many visitors) plus the
  passport-based basic KYC attributes captured at onboarding: full name,
  passport number (unique), date of birth, gender, nationality, address,
  email, phone number, date in / date out of the country, marital status,
  reason for travel, underlying condition/prescribed-medicine notes
  (`underlyingConditions`, nullable), a face photo upload (`facePhotoUrl`),
  and next of kin (name + phone) — aligned with the e-portal ("Kenya Cares")
  onboarding data set required by the framework. `Gender` and `MaritalStatus`
  are string-mapped enums. `dateIn`/`dateOut` is where the mandated cover
  period actually gets enforced: `VisitorServiceImpl` fetches the visitor's
  policy and rejects a create/update where `dateOut` is before `dateIn`, or
  where the day span between them falls outside the policy's `PolicyType`
  range — `IllegalArgumentException` (→ 400) either way.
  `GET /api/v1/visitors/{id}` and
  `GET /api/v1/visitors/by-passport?passportNumber=…` return a
  `VisitorDetailResponse` that embeds the visitor's assigned benefits
  (`visitorBenefits`), and `GET /api/v1/visitors/by-policy?policyId=…`
  returns a list of them (one per visitor on the policy) — composed in
  `VisitorController` from `VisitorBenefitService`, which keeps service
  dependencies acyclic. The paged list and create/update endpoints return
  plain `VisitorResponse` rows without benefits.
- A visitor carries a `VisitorStatus` with guarded transitions
  (`canTransitionTo`). A newly created visitor defaults to `ACTIVE`. It is updated via
  `PATCH /api/v1/visitors/{id}/status` or
  `PATCH /api/v1/visitors/by-passport/status?passportNumber=…`, both taking a
  `VisitorStatusUpdate` body; an allowed transition publishes a
  `VisitorStatusChangedEvent`, an invalid one is rejected with `409 Conflict`.
- Visitors are auto-assigned the full benefit catalog on creation:
  `VisitorServiceImpl` publishes an in-process `VisitorCreatedEvent`, which
  `visitorbenefit.VisitorCreatedListener` consumes to create one
  `VisitorBenefit` per global `Benefit` (each snapshotting the catalog
  `limitAmount` and taking the visitor's current status, `ACTIVE` by default).
  The listener skips benefits already
  assigned to the visitor, so it is idempotent. Further benefits can still be
  attached explicitly via the `VisitorBenefit` endpoints.
- A **VisitorBenefit** assigns a global catalog benefit to a visitor. It
  carries `visitorId`, `benefitId`, and its own `limitAmount` — snapshotted
  from the `Benefit` at assignment time unless an explicit limit is supplied —
  so later catalog edits do not alter benefits already assigned to a visitor.
  The referenced benefit only needs to exist (no policy-membership check). A
  visitor may hold each catalog benefit at most once (`visitorId` + `benefitId`
  unique). Usage tracking against the limit is out of scope for now.
  `VisitorBenefitResponse` additionally carries the catalog benefit's
  `benefitName` (resolved through `BenefitService`; `null` if the catalog
  benefit has since been deleted) so clients can display assignments without
  extra lookups.
- A **Preauthorization** is raised by a `PROVIDER_USER` before rendering a
  service and is decided by an `INSURER_USER` (or a admin agent).
- A **Claim** is the request for payment. It is either provider-submitted
  against an approved pre-authorization, or customer-submitted for
  reimbursement (no pre-authorization). Decisions are made by the insurer;
  `PAID` is the terminal status.
- Cross-feature references are **ID columns only** (the same rule as
  `User.organizationId`): the `claim` feature calls `PolicyService` and
  `BenefitService`, never their repositories, and no JPA relations cross
  package boundaries.

## Users, Roles & Organizations

A single `User` entity serves everyone — admin staff, insurer staff, and
service provider staff. Users are distinguished by role, not by separate
entities:

- `User.roles` is a `Set<Role>` (enum, `@ElementCollection`) mapped to Spring
  Security authorities.
- Users belonging to an external organization carry a plain
  `organizationId: UUID` together with the discriminating role
  (`INSURER_USER` → ID of an `Insurer`, `PROVIDER_USER` → ID of a
  `ServiceProvider`). This is a plain column, **not** a JPA relation, so the
  `user` package stays decoupled from `insurer` and `serviceprovider`.
- Data scoping is enforced in the service layer: for example, an
  `INSURER_USER` may only see policies and claims where
  `insurerIds` contains `user.organizationId`. Roles gate *which endpoints* a user can
  call; `organizationId` gates *which rows* they can see.
- The `auth` feature owns login and JWT concerns and depends on `user`
  (service → service); `config/SecurityConfig` wires the JWT filter and
  role-based route rules.

## Layering Rules

- **Controller → Service → Repository**. Never skip a layer or call backwards.
- Every service is split into an **interface** (`XxxService`) and an
  **implementation** (`XxxServiceImpl`, annotated `@Service`). Controllers and
  other features depend only on the interface; the implementation is an
  injection-time detail. All business logic lives in the implementation.
- Controllers accept and return **DTOs only** — entities never cross the web
  boundary.
- Mappers convert between entities and DTOs so persistence details stay inside
  the feature.
- Cross-feature calls go **service → service** (for example,
  `ServiceProviderServiceImpl` may use `InsurerService`, never
  `InsurerRepository`).
- `common/` and `config/` must not depend on any feature package.

## Base Entity, Auditing & Soft Delete

Every entity extends `common/domain/BaseEntity` (`@MappedSuperclass`):

| Field | Type | Notes |
|---|---|---|
| `id` | `UUID` | Primary key, generated |
| `deleted` | `boolean` | Soft-delete flag, defaults to `false` |
| `createdDate` | `Instant` | `@CreatedDate` |
| `updatedDate` | `Instant` | `@LastModifiedDate` |
| `deletedDate` | `Instant` | Set when `deleted` flips to `true` |
| `createdBy` | `UUID` | `@CreatedBy` — public ID of the acting user |
| `updatedBy` | `UUID` | `@LastModifiedBy` — public ID of the acting user |

- Auditing is driven by `JpaAuditingConfig`, whose `AuditorAware<UUID>` reads
  the current user's public ID from the Spring Security context.
- **Soft delete**: repositories never hard-delete. Entities are annotated with
  `@SQLDelete(sql = "... set deleted = true, deleted_date = now() ...")` and
  `@SQLRestriction("deleted = false")`, so deleted rows drop out of queries
  automatically.

## Conventions

- Package names are lowercase and singular (`user`, `insurer`,
  `serviceprovider`).
- REST base paths are plural kebab-case (`/api/v1/users`,
  `/api/v1/service-providers`).
- Request DTOs are validated with `jakarta.validation` (`@Valid` in
  controllers).
- All errors are normalized to `ApiError` by `GlobalExceptionHandler`, always
  serialized as `application/json` regardless of the request's negotiated content
  type (so error bodies never fail on non-JSON `Accept`/path extensions).
- Database schema changes ship as Flyway migrations
  (`src/main/resources/db/migration/V###__description.sql`); Hibernate
  `ddl-auto` is never used to manage the schema.

## REST Resources

| Feature           | Base Path                     | Entity Table        |
|-------------------|-------------------------------|---------------------|
| User              | `/api/v1/users`               | `users`             |
| Insurer           | `/api/v1/insurers`            | `insurers`          |
| Service Provider  | `/api/v1/service-providers`   | `service_providers` |
| Policy            | `/api/v1/policies`            | `policies`          |
| Benefit           | `/api/v1/benefits`            | `benefits`          |
| Visitor           | `/api/v1/visitors`            | `visitors`          |
| Visitor Benefit   | `/api/v1/visitor-benefits`    | `visitor_benefits`  |
| Pre-authorization | `/api/v1/preauthorizations`   | `preauthorizations` |
| Claim             | `/api/v1/claims`              | `claims`            |

## Messaging (RabbitMQ)

- Uses `spring-boot-starter-amqp`, with exchanges, queues, and bindings
  declared in `config/RabbitConfig.java`.
- Services publish domain events through `common/messaging/EventPublisher`
  (a thin wrapper over `RabbitTemplate`) — for example `claim.approved`,
  `preauthorization.decided`, and `policy.activated` — to drive notifications
  and downstream integrations.
- Listeners (`@RabbitListener`) live inside the feature package that consumes
  the event.

## Notifications (Policy Document Email)

When a `Visitor`'s cover becomes `ACTIVE`, the `notification` package
emails them a personalized policy certificate as a PDF attachment:

- `VisitorActivatedNotificationListener` sends the certificate on two paths,
  both gated on `ACTIVE`: `VisitorStatusChangedEvent` with `newStatus == ACTIVE`
  (a transition), and `VisitorCreatedEvent` when the newly created visitor is
  already `ACTIVE` (the default status), so visitors created active still get a
  certificate without a separate activation step. Unlike
  the sibling `visitorbenefit.VisitorStatusChangedListener` (which stays
  synchronous and in-transaction because it must mirror the status onto
  `VisitorBenefit` rows consistently), this listener uses
  `@TransactionalEventListener(phase = AFTER_COMMIT)`: sending mail over SMTP
  inside the same transaction that changed the visitor's status would risk
  rolling back a legitimate status change if the mail server is slow or
  unreachable. Any failure here is caught and logged, never propagated — a
  broken mail server must never affect the visitor status API's correctness.
  Re-activation (e.g. `ACTIVE` → `SUSPENDED` → `ACTIVE`) intentionally
  re-sends the certificate; that's treated as a new, valid activation, not a
  duplicate to guard against.
- The listener composes data via `VisitorService`, `PolicyService`,
  `VisitorBenefitService`, and `InsurerService` (the same "fan-in at a
  boundary" shape already used for `VisitorDetailResponse`), builds a
  `PolicyDocumentData` holder (internal to
  the package, not a DTO — it never crosses the web boundary), and passes it
  to `PolicyDocumentRenderer`.
- `PolicyDocumentRenderer` has no dependency on any other feature's service —
  it only knows how to render `templates/policy-document.html` (Thymeleaf) to
  HTML, then converts that HTML to PDF bytes via `openhtmltopdf`. The
  template deliberately excludes `Visitor.underlyingConditions`: none of the
  real insurer certificates this template is modeled on embed a free-text
  medical-conditions field, and there's no reason to widen PII exposure over
  email with it (see `policy-document-analysis.md` for the full reference
  analysis).
- `common/email/EmailService` is a thin, domain-agnostic wrapper over
  `JavaMailSender` (mirrors `common/messaging/EventPublisher`'s catch-and-log
  style) — it never logs the email body or PDF bytes, only the outcome.
- SMTP config (`spring.mail.*`) is sourced from `SMTP_*` env vars with
  STARTTLS explicitly required (Spring Boot does not enable it by default);
  `app.mail.from` and `app.mail.emergency-assistance.{phone,email}`
  (`config/MailProperties.java`) hold the small amount of static content our
  domain model doesn't capture (a 24/7 helpline is not stored per policy —
  every reference insurer hardcodes it too).
- No "document sent" tracking column exists — a resend on re-activation is
  desired behavior, not a defect.

## API Documentation (Swagger)

- Uses `springdoc-openapi-starter-webmvc-ui`: the UI is served at
  `/swagger-ui.html` and the spec at `/v3/api-docs`.
- `config/OpenApiConfig.java` holds the API metadata and the JWT bearer
  security scheme, so the **Authorize** button works in the UI.
- Both endpoints are permitted in `SecurityConfig` for non-production profiles
  only.

## Security

- The JWT signing key is a strong secret of at least 256 bits, supplied via an
  environment variable or secret manager — never committed to the repository
  or `application.yml`.
- Access tokens are short-lived and paired with refresh tokens (handled by the
  `auth` feature).
- Passwords are hashed with BCrypt (`PasswordEncoder` bean in
  `SecurityConfig`).

## Database Connection Pool (HikariCP)

Spring Boot's default pool is HikariCP. The sizing rule of thumb is
`pool size = (2 × CPU cores) + effective disk spindles`; more connections than
this degrades throughput rather than improving it.

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10       # Good default for a 4-core host with SSD storage
      minimum-idle: 10            # Keep the pool fixed-size; avoids resize churn
      connection-timeout: 30000   # ms
      max-lifetime: 1800000       # 30 min, below typical DB/firewall timeouts
```

Revisit `maximum-pool-size` only with load-test evidence, and ensure
PostgreSQL's `max_connections` comfortably exceeds
`pool size × application instances`.

## Code Practices

- **Thin controllers**: no business logic in controllers. They validate input
  (`@Valid`), delegate to the service, and map the result to a response.
- **Small, single-purpose methods**: aim for a maximum of ~20 lines; each
  method does one specific task. Extract private helpers rather than growing a
  method.
- **Unit tests** for every service (Mockito for collaborators) and mapper;
  controller slice tests with `@WebMvcTest` and `spring-security-test`;
  repository tests against H2.
- **Lombok** for boilerplate (`@Getter`, `@Builder`,
  `@RequiredArgsConstructor` for constructor injection). Avoid `@Data` on
  entities.

## Maven Dependencies

Parent: `spring-boot-starter-parent` 3.x. Explicit versions are only needed
where Spring Boot's dependency management does not provide one.

| Dependency | Scope | Purpose |
|---|---|---|
| `spring-boot-starter-web` | compile | REST controllers |
| `spring-boot-starter-data-jpa` | compile | Repositories, entities |
| `spring-boot-starter-validation` | compile | `@Valid` on request DTOs |
| `spring-boot-starter-security` | compile | Auth filter chain, role-based access |
| `spring-boot-starter-amqp` | compile | RabbitMQ messaging |
| `spring-boot-starter-mail` | compile | SMTP sending (`common/email/EmailService`) |
| `spring-boot-starter-thymeleaf` | compile | Policy document HTML templating |
| `io.github.openhtmltopdf:openhtmltopdf-pdfbox` | compile | HTML → PDF rendering for the emailed policy certificate |
| `org.springdoc:springdoc-openapi-starter-webmvc-ui` (2.6.x) | compile | Swagger UI + OpenAPI docs |
| `org.postgresql:postgresql` | runtime | Production database driver |
| `io.jsonwebtoken:jjwt-api` (0.12.x) | compile | JWT for `JwtTokenProvider` |
| `io.jsonwebtoken:jjwt-impl` (0.12.x) | runtime | JWT implementation |
| `io.jsonwebtoken:jjwt-jackson` (0.12.x) | runtime | JWT JSON serialization |
| `org.flywaydb:flyway-core` + `flyway-database-postgresql` | compile | Schema migrations |
| `org.projectlombok:lombok` | provided (optional) | Boilerplate reduction |
| `spring-boot-starter-test` | test | JUnit 5, Mockito, AssertJ |
| `spring-security-test` | test | `@WithMockUser`, security test support |
| `com.h2database:h2` | test | In-memory database for tests |

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-amqp</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-mail</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
    <dependency>
        <groupId>io.github.openhtmltopdf</groupId>
        <artifactId>openhtmltopdf-pdfbox</artifactId>
        <version>1.1.70</version>
    </dependency>
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>2.6.0</version>
    </dependency>

    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-database-postgresql</artifactId>
    </dependency>

    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.6</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.12.6</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.12.6</version>
        <scope>runtime</scope>
    </dependency>

    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```