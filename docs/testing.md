# Testing — AashrayNet

## Results (real runs, 9–10 Nov 2026)

| Check | Result |
|---|---|
| `mvn verify` (Java 21, Spring Boot 3.5.16, Maven 3.9) | ✅ **BUILD SUCCESS** |
| Unit + Mockito tests (Surefire) | ✅ **52 / 52 passed** |
| Integration tests (Failsafe, real MySQL 8.0.46, Flyway V1 + V2) | ✅ **16 / 16 passed** |
| **Total** | ✅ **68 / 68**, 0 failures, 0 skipped |
| Concurrency test repeated 5 times | ✅ 5 / 5 |
| JaCoCo line coverage | **90.0 % overall, 96.7 % service layer** (target ≥ 70 %) |
| Frontend `npm run lint` + `npm run build` | ✅ |
| Docker Compose stack (MySQL 8.4, backend, Nginx) | ✅ all healthy; app, API and Swagger served through Nginx; Flyway migrated on start |
| Screenshots | ✅ captured from the running Docker stack (`docs/screenshots/`) |

**Where the integration tests ran.** The build workspace has no shared Docker for Testcontainers, so the tests ran against a real empty MySQL 8.0.46 database, selected with `TEST_DB_URL`. Without that variable (CI, your laptop) the same tests start a MySQL 8.4 container automatically.

**Docker note.** Inside the build workspace, containers had no direct internet access, so the image *build* stages (Maven/npm downloads) could not run there. The runtime images were built from the already-compiled jar and frontend with an identical runtime stage, and the full compose stack was started and checked. On a normal machine or in GitHub Actions, `docker compose up --build` builds everything from source.

## Bugs the real build found (and fixes)

1. **`RateLimitFilter` had two constructors**, so Spring could not create it (the application did not start). Fixed with `@Autowired` on the production constructor.
2. **A real deadlock in concurrent check-in** (found by `ConcurrentCheckInIT`).
   - Both transactions inserted the evacuee first. The foreign-key check took a shared lock on the shelter row, then both wanted the exclusive lock to update it, and MySQL killed one with a deadlock error, so the request returned 500.
   - Fix: update the shelter row **first** (the version-checked UPDATE locks immediately), then insert the evacuee. Database deadlock errors are also retried.
   - After the fix, the test passed 5/5 repeated runs.

## Test inventory (68 tests)

| Class | Type | Tests |
|---|---|---|
| `OccupancyStatusTest`, `ShelterCapacityTest`, `GeoUtilsTest`, `NearestShelterRankingTest`, `ShortageCalculatorTest`, `SupplyRequestStateMachineTest`, `StockAndTextTest` | unit | 46 |
| `RateLimitFilterTest` | unit | 2 |
| `SupplyServiceReceiptTest`, `ShortageDetectionTest` | Mockito | 4 |
| `SecurityAndPrivacyIT` | MockMvc + MySQL | 11 |
| `AdminAndDashboardIT` | MockMvc + MySQL | 3 (user-management limits, events/shelters/stock/rejection, dashboards + public pages) |
| `FullWorkflowIT` | API workflow | 1 |
| `ConcurrentCheckInIT` | concurrency | 1 (passed 5/5 repeated runs) |

## Run it yourself

```bash
cd backend
mvn verify                                   # needs Docker running (Testcontainers starts MySQL 8.4)
# or against an existing empty MySQL 8 database:
TEST_DB_URL="jdbc:mysql://localhost:3306/test_db?serverTimezone=UTC" TEST_DB_USER=root TEST_DB_PASSWORD=secret mvn verify
# coverage report: backend/target/site/jacoco/index.html

cd ../frontend && npm ci && npm run lint && npm run build
```

Postman: `postman/AashrayNet.postman_collection.json` (29 requests).
