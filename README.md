# Inbound Travel Health Insurance — Backend

Spring Boot 3 / Java 21 backend for travel insurance policies, benefits,
pre-authorizations and claims. The architecture is documented in
[`backend-architecture.md`](backend-architecture.md) — read it before
contributing.

## Prerequisites

- Java 21 (e.g. `sdk use java 21.0.7-tem`)
- Maven 3.9+
- Docker (for local PostgreSQL and RabbitMQ)

## Run locally

```bash
# 1. Start PostgreSQL and RabbitMQ
docker compose up -d

# 2. Configure environment
cp .env.example .env          # then set a real JWT_SECRET (openssl rand -base64 48)
set -a; source .env; set +a

# 3. Run the app (Flyway migrates the schema on startup)
mvn spring-boot:run
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI spec: http://localhost:8080/v3/api-docs
- RabbitMQ console: http://localhost:15672 (travel / travel)

## Admin access

There is no public registration endpoint. A bootstrap admin is seeded by
Flyway migration `V002__seed_admin.sql`: `admin@travel.local` / `admin123`.
The admin creates all other users through the user management API. Change
the seeded password in any non-local environment.

## Quick smoke test

```bash
# Login as the seeded admin and capture the access token
TOKEN=$(curl -s -X POST localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"admin@travel.local","password":"admin123"}' | jq -r .accessToken)

curl -s localhost:8080/api/v1/policies -H "Authorization: Bearer $TOKEN"
```

## Tests

```bash
mvn verify
```

Tests run against in-memory H2 — no Docker required.

## Project layout

Package-by-feature under `com.travel.insurance`: `auth`, `user`, `insurer`,
`serviceprovider`, `policy`, `benefit`, `preauthorization`, `claim`,
`invoice`, plus shared `common/` and `config/`. Layering, conventions, and the
domain model are specified in `backend-architecture.md`.
