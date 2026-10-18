# Local verification

Verified on 18 October 2026 on Windows 11, using Temurin Java 21.0.12.1, Maven Wrapper 3.3.4 / Maven 3.9.11, Spring Boot 3.5.16, and Docker Engine 29.8.2.

## Automated build

Command: `.\mvnw.cmd -B clean verify`

Publication recheck: **BUILD SUCCESS** in 19.584 seconds on 18 October 2026. All 26 tests passed again before the initial commit.

| Suite | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| ProductServiceTest (JUnit 5 + Mockito) | 8 | 0 | 0 | 0 |
| ProductControllerTest (MockMvc) | 17 | 0 | 0 | 0 |
| ProductApiIT (real HTTP + PostgreSQL) | 1 | 0 | 0 | 0 |
| Total | 26 | 0 | 0 | 0 |

The integration test used a disposable PostgreSQL 16 container and random database/HTTP ports. It checked CRUD, decimal persistence, committed updates, invalid input, and missing ids. Testcontainers removed its temporary containers afterward.

## Packaged application smoke test

Started the dedicated Compose database with `docker compose up -d --wait`, then launched the generated executable JAR with `java -jar target/product-catalog-api-0.0.1-SNAPSHOT.jar`. This also verified that the actual application reads the ignored `.env` correctly.

Command: `pwsh -File scripts/smoke-test.ps1`

Observed results:

| Request | Status |
| --- | ---: |
| Create product | 201, with Location |
| Get product | 200 |
| List products | 200, includes created product |
| Update product | 200 |
| Read updated product in separate request | 200, changed price and stock persisted |
| Create invalid product | 400 |
| Delete product | 204 |
| Get deleted product | 404 |
| Update deleted product | 404 |
| Delete deleted product | 404 |

The smoke product was removed. The API process and only this project's database were stopped after verification. Port 8080 and 55432 had no listeners afterward. The project's volume remains available for subsequent development.

## Isolation and publication checks

- Maven's archive checksum was verified against Apache's published SHA-512 before pinning its SHA-256 in wrapper properties.
- This project's database used its own volume and `127.0.0.1:55432` mapping. Existing containers and databases were not altered.
- Testcontainers used a disposable database rather than the development database.
- Publication review covers the entire source tree and staged files, including an exact-value check that the local database password is absent.
- `.env`, local logs, build output, IDE metadata, database dumps, and private credentials are excluded from source control. `.env.example` contains only a placeholder.

## Outstanding limitations

No blocking functional issues were found. The tests print a non-failing Mockito/Byte Buddy warning about dynamic Java-agent loading on Java 21; this may need explicit agent configuration on a future JDK. The supported project runtime is Java 21.

This remains a local MVP: schema changes use Hibernate `update`, the product list is unpaginated, concurrent writes have no optimistic locking, and there is no authentication. These limitations are documented rather than expanding the requested scope. The Unix wrapper is included from the official distribution but was not executed on Windows.

## Source file inventory

```text
.gitattributes
.gitignore
.env.example
.mvn/wrapper/maven-wrapper.properties
mvnw
mvnw.cmd
pom.xml
compose.yaml
README.md
docs/verification.md
scripts/init-local.ps1
scripts/smoke-test.ps1
src/main/resources/application.properties
src/main/java/com/taha/catalog/ProductCatalogApplication.java
src/main/java/com/taha/catalog/controller/ProductController.java
src/main/java/com/taha/catalog/dto/ProductRequest.java
src/main/java/com/taha/catalog/dto/ProductResponse.java
src/main/java/com/taha/catalog/entity/Product.java
src/main/java/com/taha/catalog/exception/ApiExceptionHandler.java
src/main/java/com/taha/catalog/exception/ProductNotFoundException.java
src/main/java/com/taha/catalog/repository/ProductRepository.java
src/main/java/com/taha/catalog/service/ProductService.java
src/test/java/com/taha/catalog/ProductApiIT.java
src/test/java/com/taha/catalog/controller/ProductControllerTest.java
src/test/java/com/taha/catalog/service/ProductServiceTest.java
```

Local generated files also include ignored `.env`, `.local/` API logs, `build-verification.log`, Maven's `target/` output and reports, and Git metadata in `.git/`. They are not project source files for publication.
