# AashrayNet — Architecture (Phase 1, for review)

> AashrayNet is a portfolio project. All shelter, event and evacuee data is **demonstration data** (e.g. "Cyclone Dana 2026 (DEMO)"). It is not an official emergency-response service.

## 1. Technology versions

| Layer | Choice | Note |
|---|---|---|
| Language | Java 21 (LTS) | records, pattern matching, virtual threads not needed |
| Framework | **Spring Boot 3.5.x** (latest 3.5 patch) | ⚠ see *Decision D1* below |
| Web / Security | Spring MVC, Spring Security 6, JWT via `jjwt` 0.12.x | stateless |
| Persistence | Spring Data JPA, Hibernate 6, Flyway (managed by Boot) + `flyway-mysql` | |
| Validation | Jakarta Bean Validation (Hibernate Validator) | |
| API docs | springdoc-openapi 2.x (Swagger UI) | |
| Rate limiting | Bucket4j (in-memory) | single instance is enough for MVP |
| Database | MySQL 8.4 LTS in Docker (schema validated on MySQL 8.0.46) | |
| Frontend | React 19 + TypeScript + Vite, React Router, Tailwind CSS, Axios, Recharts, Leaflet + OpenStreetMap (optional map) | |
| Testing | JUnit 5, Mockito, Spring Boot Test, MockMvc, `@DataJpaTest`, Testcontainers (MySQL) | |
| DevOps | Maven, Docker, Docker Compose, Nginx, GitHub Actions | |

**Pinned versions:** Spring Boot 3.5.16, jjwt 0.12.6, springdoc 2.8.9, JaCoCo 0.8.13, Testcontainers (Boot BOM); React 19.3, React Router 7.18, Vite 6.4, TypeScript 5.9, Tailwind 4.3, Recharts 3.10, Axios 1.21. Rate limiting is a small in-house filter (no Bucket4j dependency).

**Decision D1 (approved: 3.5.x):** the spec asks for Spring Boot 3.x. The last 3.x line is 3.5, and its free (open-source) support ended on **30 June 2026**. The current line is Spring Boot 4.x.
- Option A (default): stay on **3.5.x**, as the spec says. Most tutorials and interview material still use 3.x, so this is better for learning. The downside is no more free security patches.
- Option B: use **Spring Boot 4.x**. It is the currently supported line, but it brings changes (Jackson 3, modular starters) that most learning material doesn't cover yet.

## 2. Style: modular monolith, layered

```
React SPA ──HTTPS──► Nginx ──► Spring Boot (one JAR) ──JDBC──► MySQL 8
                                 │
                                 ├── controller  (HTTP + DTO validation, thin)
                                 ├── service     (business rules, @Transactional)
                                 ├── repository  (Spring Data JPA)
                                 ├── security    (JWT filter, role + district checks)
                                 └── scheduler   (shortage job, snapshots)
```

**Why a monolith:** one team, one database, and one deployable. Microservices would add network calls and distributed transactions, and the problem doesn't need them. Interview line: *"I split the code by module inside one deployable, so it could be split into services later if needed."*

## 3. Request flow (example: evacuee check-in)

1. React sends `POST /api/shelters/7/evacuees` with a JWT.
2. `JwtAuthenticationFilter` validates the signature, expiry and the revoked-token list, then loads the user.
3. `@PreAuthorize("hasAnyRole('VOLUNTEER','SHELTER_INCHARGE')")` checks the role.
4. `EvacueeController` validates the DTO and calls `EvacueeService.checkIn`.
5. The service calls `AccessPolicy.assertCanOperateShelter(user, shelter)`: a volunteer must be in the same district, an in-charge must be `shelters.incharge_id`.
6. `CapacityService` (inside a transaction) loads the shelter, checks free places, saves the evacuee and runs `occupancy + 1`.
7. On commit, Hibernate runs `UPDATE shelters SET occupancy=?, version=version+1 WHERE id=? AND version=?`.
   - If another check-in committed first, 0 rows are updated and Hibernate throws `ObjectOptimisticLockingFailureException`.
   - The service retries up to 3 times. After that it returns **409 Conflict**.
   - If the shelter is full, it returns **422 SHELTER_FULL** with the nearest alternatives.

## 4. Key algorithms (detailed docs in Phase 2)

### 4.1 Occupancy status

Uses integer arithmetic to avoid floating-point mistakes:

| Rule | Status |
|---|---|
| `occupancy == capacity` | FULL |
| `occupancy*100 < 70*capacity` | GREEN |
| `occupancy*100 <= 90*capacity` | YELLOW |
| otherwise | RED |

Tests at a capacity of 100 with occupancy 69 / 70 / 90 / 91 / 100 give GREEN / YELLOW / YELLOW / RED / FULL.

### 4.2 Concurrency-safe capacity

There are three layers of protection:
1. Service check: `free >= 1`.
2. `@Version` optimistic lock, with a retry.
3. MySQL `CHECK (occupancy <= capacity)` as the last safety net.

The test runs two threads with a `CountDownLatch` against Testcontainers MySQL, with 1 free place. Exactly one thread must succeed, and the final occupancy must equal the capacity.

An alternative to discuss in interviews is an atomic `UPDATE ... SET occupancy = occupancy + 1 WHERE id = ? AND occupancy < capacity`. It is also correct, but the spec asks for `@Version`.

### 4.3 Nearest available shelter

1. A JPA query selects active shelters where `capacity - occupancy >= peopleNeeded` and `id <> current`, inside a latitude/longitude bounding box (default radius 50 km).
2. Java computes the Haversine distance (R = 6371 km) for each candidate.
3. Results are sorted ascending and the first `count` are returned.

Results may cross district borders, because shelter names and free capacity are already public. Private evacuee data never crosses districts. The engine only **recommends** shelters; it never transfers anyone.

### 4.4 Shortage

`daysRemaining = quantity / (occupancy × perPersonPerDay)` is computed only when occupancy > 0 and the rate > 0. Results:
- `< 1` → shortage
- quantity 0 → 0 days (shortage)
- a consumption-tracked item with no stock row → quantity 0

The hourly job "upserts" an alert. The DB unique key `(shelter, item, active_key)` allows only one open alert per shelter and item, so re-running the job never duplicates alerts. Alerts resolve automatically when stock covers at least 1 day again.

### 4.5 Supply workflow

`REQUESTED → APPROVED → DISPATCHED → RECEIVED`, plus `REQUESTED → REJECTED`. Transitions are enforced in `SupplyRequest.transitionTo()`. On receipt, one transaction does three things:
- moves the request to RECEIVED (with a `@Version` check)
- adds each line to `shelter_stock` (with a `@Version` check)
- inserts one `stock_movements` row per line

`UNIQUE(request_id, item_id)` on `stock_movements` makes a second receipt impossible, even if two requests arrive at the same time.

## 5. Security model (summary)

| Role | Scope |
|---|---|
| STATE_ADMIN | everything; `district_id` is NULL |
| DISTRICT_ADMIN | own district only |
| SHELTER_INCHARGE | shelters where `incharge_id = me` |
| VOLUNTEER | check-in at any active shelter of own district |
| PUBLIC | `/api/public/**` only, rate-limited, minimal fields |

District scoping is applied **in repository queries**, e.g. `findByIdAndDistrictId`. A record from another district returns **404, not 403**, so attackers can't tell whether it exists. Passwords use BCrypt (strength 12). JWT access tokens last 30 minutes and carry a `jti`; logout stores the `jti` in `revoked_tokens` until it expires.

## 6. Repository and package structure

```
aashraynet/
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/rajesh/aashraynet/
│       │   ├── AashrayNetApplication.java
│       │   ├── config/        SecurityConfig, CorsConfig, OpenApiConfig, SchedulingConfig
│       │   ├── security/      JwtService, JwtAuthenticationFilter, AppUserDetailsService,
│       │   │                  AccessPolicy (district/shelter checks), RateLimitFilter
│       │   ├── entity/        User, Role, District, DisasterEvent, Shelter, Evacuee, SpecialNeed,
│       │   │                  SupplyItem, ShelterStock(+Id), StockMovement, SupplyRequest,
│       │   │                  RequestLine(+Id), RequestStatus, ShortageAlert, AuditLog
│       │   ├── repository/    one Spring Data interface per aggregate
│       │   ├── dto/request/   LoginRequest, CheckInRequest, CreateShelterRequest, ...
│       │   ├── dto/response/  ShelterResponse, NearestShelterResponse, PublicFinderResponse, ...
│       │   ├── service/       AuthService, UserService, EventService, ShelterService,
│       │   │                  CapacityService, OccupancyCalculator, EvacueeService,
│       │   │                  NearestShelterService, GeoUtils, SupplyService, StockService,
│       │   │                  ShortageService, FamilyFinderService, DashboardService
│       │   ├── controller/    AuthController, UserController, EventController, ShelterController,
│       │   │                  EvacueeController, SupplyController, ShortageController,
│       │   │                  PublicController, DashboardController
│       │   ├── scheduler/     ShortageCheckJob, OccupancySnapshotJob
│       │   └── exception/     GlobalExceptionHandler, ApiError, BusinessRuleException,
│       │                      CapacityExceededException, ResourceNotFoundException
│       ├── main/resources/
│       │   ├── application.yml, application-dev.yml, application-prod.yml
│       │   └── db/migration/  V1__init_schema.sql, V2__demo_data.sql (dev only)
│       └── test/java/...      unit, @DataJpaTest, MockMvc, integration (Testcontainers)
├── frontend/                  Vite + React + TS (src/pages, src/components, src/api, src/auth)
├── docs/                      architecture, database-design, ER-diagram, api-documentation, ...
├── postman/                   AashrayNet.postman_collection.json
├── infrastructure/docker/     backend.Dockerfile, frontend.Dockerfile
├── infrastructure/nginx/      nginx.conf (SPA + /api proxy, security headers)
├── .github/workflows/         ci.yml (backend test + frontend lint/build)
├── docker-compose.yml, .env.example, .gitignore, LICENSE, README.md
```

## 7. Learning notes — what each layer is for

| Layer | Responsibility | Interview question |
|---|---|---|
| Controller | HTTP mapping, `@Valid` DTOs, status codes. No business logic. | "Why not return entities from controllers?" (lazy-loading errors, leaking fields like `password_hash`) |
| Service | Rules plus `@Transactional` boundaries | "Where do you put `@Transactional` and why?" |
| Repository | Queries only | "What is the N+1 problem? How do `JOIN FETCH` and `@EntityGraph` fix it?" |
| Security | Authentication (who you are) and authorisation (what you may do) | "Explain the JWT filter chain." |
| Scheduler | Time-based jobs that are idempotent | "What happens if the job runs twice?" |
