# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

QAGenie Test Automation Platform - Backend. Spring Boot 4 / Java 21 / Oracle (with H2 fallback for local dev). A platform where users onboard applications, pull their OpenAPI/Swagger specs, track spec drift over time, and (per the wider product) generate/manage test scenarios, test data, flows, and executions against them.

## Commands

```bash
mvn clean compile              # compile
mvn spring-boot:run            # run locally (defaults to dev profile, H2 in-memory DB)
mvn test                       # run all tests
mvn test -Dtest=ClassName      # run a single test class
mvn test -Dtest=ClassName#methodName   # run a single test method
mvn clean package              # build the jar (eurotech-hackathon-be.jar)
```

There are currently no test sources under `src/test` — don't assume test coverage exists for existing code.

### Local run configuration

The dev profile (`application-dev.properties`) defaults `USE_ORACLE_DB=false` and `JWT_ENABLED=false`, so `mvn spring-boot:run` works out of the box against an in-memory H2 DB with auth disabled (every anonymous request is granted all three roles — see Security below). Relevant env vars, all optional locally:

- `USE_ORACLE_DB` — `true` to connect to a real Oracle instance via `spring.datasource.*` in `application-dev.properties`; schema must already exist (`ddl-auto=validate`), see `db-scripts/`. `false` lets Hibernate create the schema from JPA entities directly against H2.
- `JWT_ENABLED` — `true` to enforce real JWT auth; `false` bypasses it entirely (see `SecurityConfig`).
- `DB_USER` / `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRY`, `CORS_ORIGINS`, `CERTS_STORAGE_DIR`, `SWAGGER_SUFFIX`.

Swagger UI: `/swagger-ui.html`. OpenAPI docs: `/v3/api-docs`.

## Architecture

### Package-by-feature

Code is organized by domain feature under `com.qagenie.testbe`, not by technical layer: `project/`, `application/`, `environment/`, `envvariable/`, `changetracker/`, `scenario/`, `testdata/`, `testflow/`, `execution/`, `report/`, `dashboard/`, `user/`, `auth/`, `security/`, `config/`, `common/`. Each feature package follows the same internal layout: `controller/ → service/ (interface) → service/impl/ → repository/`, plus `entity/`, `dto/`, `mapper/` (MapStruct). Follow this shape when adding a new feature.

### Domain hierarchy

```
PROJECT              top-level grouping; owns shared keystore/truststore + credential config
  ├─ ENVIRONMENT_CONFIG   Dev/Staging/Prod base URLs (also PROJECT_ENV_VARIABLE: simple name/value pairs)
  └─ APPLICATION          onboarded once; derives its spec URL from a chosen ENVIRONMENT_CONFIG
                          + its own name + the project's SPEC_PATH_SUFFIX, or uses a custom URL
       ├─ SPEC_VERSION    hash-guarded version history; exactly one CURRENT, new fetches land
       │                  PENDING until a human approves/rejects (see Change Tracker below)
       ├─ TEST_SCENARIO   remembers which SPEC_VERSION it was generated against (nullable for manual)
       ├─ TEST_DATA
       ├─ TEST_FLOW / TEST_FLOW_STEP   ordered sequences of scenarios
       └─ EXECUTION_RUN / EXECUTION_RESULT   runs against one of the project's ENVIRONMENT_CONFIGs
```

Reference DDL lives in `db-scripts/01_schema.sql` (Oracle, run once per environment) and `db-scripts/02_roles_and_users.sql` (seed users). Table/column names are Oracle-style upper-snake-case; JPA entities map to them explicitly. Deleting a PROJECT cascades to everything under it.

### Spec fetch → diff → change tracker flow

This is the most cross-cutting piece of logic in the codebase, spanning three packages:

1. **`application.service.SpecFetchService`** — performs the outbound HTTP(S) call to retrieve a spec from a resolved URL, honoring the owning `Project`'s `specAuthType`/`specAuthConfigJson` (NONE/BASIC/BEARER/API_KEY/MUTUAL_TLS). Mutual TLS uses per-project keystore/truststore files managed by `project.tls.TlsMaterialService` (stored on disk under `qagenie.certs.storage-dir`, never in the DB — path only).
2. **`ApplicationService`** (impl in `application.service.impl`) — hash-guards fetched content against `SPEC_VERSION.CONTENT_HASH`; unchanged content is a no-op, changed content lands as a new `PENDING` `SpecVersion` alongside the existing `CURRENT` one.
3. **`application.diff.SpecDiffService`** — best-effort structural diff between two OpenAPI/Swagger documents (JSON or YAML) at "HTTP method + path" granularity (`diff()`), plus a field-level breakdown per changed endpoint including rename detection via type/format heuristics (`diffFields()`). Documents without a top-level `paths` object (e.g. a frontend DOM snapshot) fall back to a whole-document ADDED/MODIFIED entry. This is intentionally not a full semantic OpenAPI diff — no `$ref` resolution.
4. **`changetracker.ChangeTrackerService`** — thin orchestration wrapper: `analyze()` triggers a fetch (already hash-guarded/versioned by step 2), `listPendingVersions()`/`getPendingImpact()` surface PENDING versions with diffs attached, `heal()` approves a pending version (promotes it to CURRENT, supersedes the old one).

When touching spec-fetch, diffing, or the approval workflow, changes usually need to be coordinated across these three packages rather than made in isolation.

### Security

- Stateless JWT auth (`security/JwtAuthFilter`, `JwtTokenProvider`), three roles only: `ADMIN` (full access incl. user management), `TESTER` (full access, no user management), `VIEWER` (read-only). Enforced with `@PreAuthorize` at the controller layer, next to each operation — check the annotation on a method rather than assuming REST-verb defaults (e.g. Project TLS config and deletion are ADMIN-only even though other Project reads are open to all authenticated roles).
- `qagenie.security.jwt.enabled=false` (dev default) disables the JWT filter chain entirely and grants anonymous callers every role, so `@PreAuthorize` checks still pass without minting tokens. Don't rely on this bypass existing in prod-like configs — flip `JWT_ENABLED=true` when testing real auth behavior.
- `qagenie.datasource.use-oracle-db=true` is the prod-like default in `application.properties`; only the dev profile flips it to `false`.

### Cross-cutting conventions

- **API responses**: every controller returns `ApiResponse<T>` (`common.response.ApiResponse`) — `{success, message, data, timestamp, errorCode}`. Paginated results wrap `Page<T>` in `common.response.PageResponse` via `PageResponse.from(page)`.
- **Errors**: thrown as `BusinessException` (422, custom `errorCode`) or `ResourceNotFoundException` (404); anything else funnels through `common.exception.GlobalExceptionHandler`'s generic handler as a 500. Add new domain error types as subclasses/usages of these rather than handling exceptions ad hoc in controllers.
- **Auditing**: entities needing created/updated tracking extend `common.audit.AuditableEntity` (`CREATED_AT/BY`, `UPDATED_AT/BY` via Spring Data JPA auditing, configured in `config.JpaAuditingConfig`).
- **Controller logging**: `common.aspect.ControllerLoggingAspect` is an AOP `@Around` advice on every `@RestController` that logs entry/exit with a correlation id, redacting `password`/`secret`/`token`-named fields and multipart payloads automatically — controllers don't need their own logging boilerplate.
- **Mapping**: entity ↔ DTO conversion uses MapStruct (`mapper/` packages, `@Mapper` interfaces), not hand-written mapping code.
