# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./gradlew build

# Run (requires PostgreSQL)
./gradlew bootRun

# Run only the database via Docker, then boot locally
docker compose up postgres -d
./gradlew bootRun

# Run all tests (uses Testcontainers — no local DB needed)
./gradlew test

# Run a single test class
./gradlew test --tests "br.com.hadryan.agro.manager.AuthIntegrationTest"

# Run a single test method
./gradlew test --tests "br.com.hadryan.agro.manager.AuthIntegrationTest.shouldLoginSuccessfully"

# Full stack via Docker Compose
docker compose up -d
```

API runs at `http://localhost:8080`. Swagger UI at `/swagger-ui.html`.

## Environment Variables

| Variable | Description |
|---|---|
| `DB_HOST` | PostgreSQL host (default: `postgres`) |
| `DB_NAME` | Database name (default: `agro_manager`) |
| `DB_USERNAME` | Database user |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | JWT signing key (min 64 chars) |
| `GOOGLE_CLIENT_ID` | Google OAuth2 client ID |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 client secret |
| `FRONTEND_URL` | Frontend URL for CORS (e.g. `http://localhost:5173`) |

## Architecture

**Package-by-feature** under `domain/`. Each domain package contains its entity, repository, request/response DTOs, service interface, service implementation, and controller(s) co-located together.

### Service Pattern

All services follow interface + implementation: `FooService` (interface) + `FooServiceImpl` (annotated `@Service`). Always inject the interface, never the implementation.

### Multi-tenancy

Resources are scoped to `Account`. `AccountMember` maps `User` → `Account` with a role (`OWNER`/`ADMIN`/`MEMBER`). Most endpoints are nested under `/accounts/{accountId}/...`. Authorization checks happen inside service methods by verifying the caller's `AccountMember` record.

### Security

- Stateless JWT via `JwtAuthenticationFilter` (reads `Authorization: Bearer <token>`)
- `JwtService` generates/validates tokens using HMAC-SHA256; signing key is computed once at startup via `@PostConstruct`
- OAuth2 Google handled by `CustomOAuth2UserService` + `OAuth2AuthenticationSuccessHandler`
- Public routes: `/auth/**`, `/oauth2/**`, `/login/oauth2/**`, `/invites/**` (GET), Swagger, Actuator health/prometheus
- Custom auth entry point returns 401 JSON for API clients (`Bearer`/`application/json`) and redirects browsers to Google OAuth

### Shared Layer

- `ApiResponse<T>` — uniform response envelope (`data`, `message`, `success`)
- `PageResponse<T>` — pagination wrapper
- `BusinessException(message, HttpStatus)` — domain rule violations
- `ResourceNotFoundException` — 404-style errors
- `GlobalExceptionHandler` — maps all exceptions to `ApiResponse`; only unexpected errors are logged with stack trace

### Testing

- `IntegrationTestBase` — `@SpringBootTest` + Testcontainers PostgreSQL 17; shared static container across all subclasses; `JavaMailSender` and `SpringTemplateEngine` are `@MockitoBean` (no SMTP needed)
- `MockMvcIntegrationTestBase extends IntegrationTestBase` — adds `MockMvc` + helpers (`registerAndGetToken`, `createAccount`, `createFarm`, `createExpense`)
- Integration tests live in the root test package; unit tests live in `domain/<feature>/`
- Use `uniqueEmail()` helper when tests register users to avoid email collision between parallel runs