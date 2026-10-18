# Product Catalog API

A Java 21 and Spring Boot REST API for managing product names, prices, and stock quantities in PostgreSQL. Built as a local learning and portfolio MVP for PFE 2027 backend engineering applications; it is not a production-deployed application.

## Features

- Create, list, retrieve, update, and delete products.
- Validate input and return explicit HTTP statuses, including `201`, `400`, and `404`.
- Store prices as `BigDecimal` with PostgreSQL `numeric(12,2)` precision.
- Test service behavior with Mockito, HTTP handling with MockMvc, and the full API with real PostgreSQL through Testcontainers.

## Technology stack

Java 21 · Spring Boot 3.5.16 · Maven 3.9.11 / Maven Wrapper · Spring Web · Spring Data JPA · PostgreSQL 16 · Jakarta Bean Validation · JUnit 5 · Mockito · Testcontainers

## Architecture

```text
HTTP JSON -> Controller -> Service -> Repository -> PostgreSQL
```

The [application packages](src/main/java/com/taha/catalog) separate HTTP routing and validation (`controller`), application operations and transactions (`service`), persistence access (`repository`), database mappings (`entity`), request/response records (`dto`), and centralized error responses (`exception`). DTOs keep client input separate from persistence ids. Constructor injection makes dependencies explicit.

Read operations use read-only transactions; writes use write transactions. Updates change a managed entity and rely on JPA dirty checking to persist it at commit. See [learning notes](docs/learning-notes.md) for an implementation exercise and interview preparation.

## API

Base URL: `http://127.0.0.1:8080`

| Method | Path | Success | Expected errors |
| --- | --- | --- | --- |
| POST | `/api/products` | 201, body and Location header | 400 invalid input |
| GET | `/api/products` | 200, array ordered by id; empty catalog is `[]` | — |
| GET | `/api/products/{id}` | 200, body | 404 missing product; 400 nonnumeric id |
| PUT | `/api/products/{id}` | 200, updated body | 400 invalid input; 404 missing product |
| DELETE | `/api/products/{id}` | 204, no body | 404 missing product |

POST and PUT accept a complete request; responses also include a generated `id`:

```json
{"name":"Keyboard","price":49.95,"stockQuantity":4}
```

Names must be nonblank and at most 120 characters; surrounding whitespace is stripped. Prices are required, nonnegative, and limited to ten integer digits and two fractional digits. Stock is a required nonnegative 32-bit integer; fractional stock is rejected. Zero price and stock are allowed. PUT replaces all editable fields and does not create a missing product.

Validation failures return Problem Detail with an `errors` map keyed by field; missing products also return Problem Detail. Malformed JSON and incompatible path values return Spring MVC's standard 400 response.

## Run locally on Windows

Prerequisites: Java 21, Git, PowerShell 7, and Docker Desktop running Linux containers. Global Maven is unnecessary. The first build requires internet access for Maven, dependencies, and Docker images.

```powershell
git clone https://github.com/taha-qertout/product-catalog-api.git
cd product-catalog-api
pwsh -File scripts/init-local.ps1
docker compose up -d --wait
.\mvnw.cmd spring-boot:run
```

The [setup script](scripts/init-local.ps1) generates a random password in ignored `.env` without overwriting an existing file. Alternatively copy [.env.example](.env.example) to `.env` and replace the placeholder. Compose and Spring both read `.env` from the repository root; use plain `KEY=value` syntax without quotes. Environment variables can override these values.

Retrieve products with `Invoke-RestMethod http://127.0.0.1:8080/api/products`. In IntelliJ, open `pom.xml`, select JDK 21 and the Maven Wrapper, and run `ProductCatalogApplication` with the repository root as working directory.

The API binds to localhost on port 8080. The dedicated Compose project `taha-product-catalog-api` uses its own database and named volume, with PostgreSQL bound to `127.0.0.1:55432`. It does not reuse an existing database on port 5432. If 55432 or 8080 is occupied, change `DB_PORT` or `SERVER_PORT` in `.env` before starting.

Stop the API with Ctrl+C and the project database with `docker compose down`. Data persists in the named volume: `down -v` deletes that data. Changing `.env` after initialization does not change the password stored by PostgreSQL.

**Keep secrets local:** never commit `.env`, credentials, database dumps, IDE metadata, logs, or generated build output. These are covered by [.gitignore](.gitignore); `.env.example` contains only a placeholder. The Maven Wrapper verifies its pinned Maven distribution with SHA-256.

## Tests and verification

Run from the repository root:

```powershell
# Service and MockMvc tests; Docker is not required
.\mvnw.cmd test

# Complete build including real HTTP + PostgreSQL integration test; Docker required
.\mvnw.cmd verify

# Smoke test with the development database and API already running
pwsh -File scripts/smoke-test.ps1
# If the API uses another port
pwsh -File scripts/smoke-test.ps1 -BaseUrl http://127.0.0.1:18080
```

The [test sources](src/test/java/com/taha/catalog) cover CRUD behavior, validation, missing products, and committed updates. The integration test uses a disposable PostgreSQL container and random ports, never the development database; it fails if Docker is unavailable. Reports are written to `target/surefire-reports` and `target/failsafe-reports`. The [smoke script](scripts/smoke-test.ps1) creates a uniquely named product and removes only that product.

See [recorded verification evidence](docs/verification.md) for the previously executed tests and smoke checks. Those results are historical evidence, not a claim that tests run automatically on each change.

## Current limitations

- Local MVP with no authentication or production deployment; public deployment requires a separate security and deployment review.
- Hibernate `ddl-auto=update` simplifies local setup but does not provide versioned schema migrations.
- Listing is unpaginated; concurrent updates have no optimistic locking and use last-write-wins behavior.
- Prices have no currency field; the catalog assumes a single currency.
