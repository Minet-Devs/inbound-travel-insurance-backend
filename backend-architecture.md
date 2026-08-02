# Travel Insurance — Backend Architecture

This document describes the architecture of the Inbound Travel Medical Insurance backend.
It is the reference for how the codebase is organized, how the domain model fits
together, and the conventions every contribution is expected to follow. Read it
before writing your first feature.

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
│   └── RabbitConfig.java                   # Exchanges, queues, bindings
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
│   └── 📁 util/
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
│   ├── Policy.java                         # insurerIds (set), cover dates
│   ├── PolicyStatus.java                   # Enum: DRAFT, ACTIVE, EXPIRED, CANCELLED
│   ├── PolicyMapper.java
│   └── 📁 dto/
│       ├── PolicyRequest.java
│       └── PolicyResponse.java
│
├── 📁 benefit/                             # Feature: Benefit Catalog
│   ├── BenefitController.java
│   ├── BenefitService.java                 # Interface
│   ├── BenefitServiceImpl.java
│   ├── BenefitRepository.java
│   ├── Benefit.java                        # policyId, name, limitAmount, usedAmount
│   ├── BenefitMapper.java
│   └── 📁 dto/
│       ├── BenefitRequest.java
│       └── BenefitResponse.java
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
Policy ──1:N── Benefit                    (a policy carries a set of benefits with limits)
   │
   ├──1:N── Preauthorization ──0:1── Claim
   │            (provider asks for approval  (a claim may reference the
   │             before rendering a service)  pre-authorization that authorized it)
   └──1:N── Claim                          (claims may also arrive without a
                                            pre-authorization, e.g. reimbursement
                                            of out-of-pocket costs)
```

- A **Policy** is the insurance contract. It references a set of backing
  insurers (`insurerIds`) and carries cover dates and a status. It holds no
  treatment-level detail.
- **Benefit** rows belong to a policy and track `limitAmount` against
  `usedAmount`. Approving a pre-authorization or claim draws down the benefit
  via `BenefitService`.
- A **Preauthorization** is raised by a `PROVIDER_USER` before rendering a
  service and is decided by an `INSURER_USER` (or a admin agent). Approval
  reserves an amount against the benefit.
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
- All errors are normalized to `ApiError` by `GlobalExceptionHandler`.
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