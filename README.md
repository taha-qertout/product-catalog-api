# Product Catalog API

A small Java 21 / Spring Boot REST API built as a learning and backend portfolio project for PFE 2027 applications. It manages products in PostgreSQL with explicit HTTP behavior, validation, and automated tests.

## Stack and scope

Java 21, Spring Boot 3.5.16, Maven 3.9.11 (Wrapper), Spring Web, Spring Data JPA, Jakarta Bean Validation, PostgreSQL 16, JUnit 5, Mockito, and Testcontainers.

Spring Boot 3.5.16 is a stable maintenance release compatible with Java 21; see the [official system requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html).

This MVP deliberately focuses on five CRUD operations. It has no authentication and binds to localhost. It is intended for local development; public deployment needs a separate security and deployment review.

## Run on Windows

Prerequisites: Java 21, PowerShell 7, and Docker Desktop with Linux containers running. A global Maven installation is unnecessary. First build needs internet access for Maven, dependencies, and Docker images.

From this directory:

```powershell
pwsh -File scripts/init-local.ps1
docker compose up -d --wait
.\mvnw.cmd spring-boot:run
```

The initialization script creates a random password in ignored `.env` and never overwrites an existing file. Alternatively copy `.env.example` to `.env` and replace the placeholder. Compose reads `.env`; Spring reads it as a properties file from the working directory. Keep values in plain `KEY=value` syntax without quotes. Environment variables can override file values.

Open `http://127.0.0.1:8080/api/products`. In IntelliJ, open `pom.xml`, select JDK 21 and the Maven Wrapper, and run `ProductCatalogApplication` with the project root as working directory.

If port 55432 or 8080 is occupied, edit `DB_PORT` or `SERVER_PORT` in `.env` before starting. Keep the existing database on 5432 unchanged.

Stop the API with Ctrl+C. Stop only this project's database with:

```powershell
docker compose down
```

Data persists in this project's named volume. Avoid `down -v` unless you intend to delete that data. Changing the password in `.env` after PostgreSQL initializes does not change the stored database password.

## Database isolation and secrets

Compose project `taha-product-catalog-api` owns its own database service and `catalog-data` volume. PostgreSQL binds to `127.0.0.1:55432`, so it does not reuse an existing container, database, or port 5432. There is no fixed container name, external volume, or host data-directory mount. Testcontainers uses a separate disposable PostgreSQL instance with a random port and temporary credentials; it never connects to the development database.

No real password belongs in source control. `.env`, `.local`, IDE metadata, build output, and logs are ignored. `.env.example` contains only a placeholder. Maven Wrapper verifies its pinned Maven distribution with SHA-256.

## API contract

| Method | Path | Success | Other expected responses |
| --- | --- | --- | --- |
| POST | `/api/products` | 201, body and Location header | 400 invalid input |
| GET | `/api/products` | 200, array ordered by id | Empty catalog is `[]` |
| GET | `/api/products/{id}` | 200, body | 404 missing product, 400 nonnumeric id |
| PUT | `/api/products/{id}` | 200, updated body | 400 invalid input, 404 missing product |
| DELETE | `/api/products/{id}` | 204, no body | 404 missing product |

POST and PUT accept the same complete request:

```json
{"name":"Keyboard","price":49.95,"stockQuantity":4}
```

The response adds a database-generated `id`. Names must be nonblank and at most 120 characters; surrounding whitespace is stripped. Prices are required, nonnegative, have at most ten integer digits and two fractional digits. Stock is a required nonnegative 32-bit integer; fractional stock is rejected. Zero price and stock are allowed. Prices have no currency field: treat this as a single-currency catalog. PUT replaces all editable fields; it does not create a missing product.

Validation failures use a Problem Detail response with an `errors` map keyed by field. Missing products also return Problem Detail. Malformed JSON and incompatible path values return Spring MVC's standard 400 response.

PowerShell example:

```powershell
$body = @{name='Keyboard'; price=49.95; stockQuantity=4} | ConvertTo-Json
Invoke-RestMethod http://127.0.0.1:8080/api/products -Method Post -ContentType application/json -Body $body
Invoke-RestMethod http://127.0.0.1:8080/api/products
```

## Architecture and important choices

```text
HTTP JSON -> Controller -> Service -> Repository -> PostgreSQL
                DTOs        |           |
                            +--- Entity-+
```

All Java packages are under `src/main/java/com/taha/catalog`:

- `controller/ProductController`: routes, request validation, and HTTP status/Location handling.
- `dto/ProductRequest` and `ProductResponse`: immutable Java records defining the public JSON contract. Separate DTOs prevent clients from controlling persistence ids.
- `service/ProductService`: application operations, DTO mapping, and transaction boundaries. Constructor injection makes dependencies explicit and easy to mock.
- `repository/ProductRepository`: Spring Data generates the standard database operations from `JpaRepository`.
- `entity/Product`: database mapping; `BigDecimal` maps to `numeric(12,2)` to avoid binary floating-point money errors. A protected no-argument constructor is required by JPA.
- `exception`: one missing-product exception and centralized HTTP error mapping.

Read methods use read-only transactions. Create, update, and delete use write transactions. Update loads a managed entity and changes its fields; JPA dirty checking writes those changes when the transaction commits. An extra `save()` is unnecessary for that managed entity. Delete loads first so a missing id has a predictable 404 response.

`ddl-auto=update` keeps this learning MVP simple. It is a development convenience, not versioned schema management; use explicit migrations before deploying or evolving a shared database. There is no pagination or concurrency/version control yet. The list endpoint suits a small catalog; simultaneous writes currently use last-write-wins behavior.

## Tests and verification

```powershell
# Fast tests, no Docker needed:
.\mvnw.cmd test

# Full build, including real PostgreSQL and HTTP integration test; Docker required:
.\mvnw.cmd verify

# With the development database and API running:
pwsh -File scripts/smoke-test.ps1
# For a different API port:
pwsh -File scripts/smoke-test.ps1 -BaseUrl http://127.0.0.1:18080
```

`ProductServiceTest` uses JUnit 5 and Mockito to test service behavior, mapping, updates, deletion, and missing ids. `ProductControllerTest` uses MockMvc to test status codes, Location, validation boundaries, malformed input, and rejection before service calls. `ProductApiIT` starts the complete application on a random HTTP port against real PostgreSQL; it exercises CRUD, committed update persistence, validation, and 404 behavior. Integration tests fail if Docker is unavailable rather than silently skipping.

Surefire reports are in `target/surefire-reports`; Failsafe reports are in `target/failsafe-reports`. The smoke script creates a uniquely named product and removes only that product. See `docs/verification.md` for the actual local verification results and limitations.

## Your learning task

The MVP is complete so you have a working reference. Before applying with it, own a small piece of it:

1. Read the controller, then hide the implementation of `ProductService.findById` and reimplement it yourself. Use `repository.findById`, handle `Optional`, throw `ProductNotFoundException` for a missing id, and map the entity to `ProductResponse`.
2. Recreate its two service tests: existing product and missing product. Run `.\mvnw.cmd test`.
3. Explain aloud why the service does not return `ResponseEntity`, why the repository returns `Optional`, and where the 404 is decided.

Then trace POST from JSON to validation to service to INSERT to the 201 response. Be ready to explain `BigDecimal`, dependency injection, transactions, dirty checking, and why an integration test catches failures that Mockito cannot. Describe this honestly as an assisted project that you understand and extended yourself.

## Repository hygiene

Never commit `.env`, credentials, database dumps, IDE metadata, or generated build output. `.env.example` contains a placeholder, and the setup script generates a local password. Run the complete test suite and review the staged diff before publishing changes. A GitHub Actions workflow, authentication, search, pagination, and other extra features are intentionally outside this MVP.
