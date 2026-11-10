# Code Walkthrough: learn the project class by class

Read in this order. For each class: **what it does → why it exists → what it depends on → who calls it → interview question.**

## 1. Entry point and configuration

| Class | What / why | Depends on | Interview question |
|---|---|---|---|
| `AashrayNetApplication` | Starts Spring Boot. `@ConfigurationPropertiesScan` loads `AppProperties`. | — | What does `@SpringBootApplication` combine? (`@Configuration`, `@EnableAutoConfiguration`, `@ComponentScan`) |
| `AppProperties` | Typed record for `app.*` settings (JWT, CORS, rate limits). Avoids `@Value` strings everywhere. | application.yml | `@ConfigurationProperties` vs `@Value`? |
| `SecurityConfig` | The security filter chain: stateless sessions, public URLs, JSON 401/403, BCrypt, CORS. It also **disables auto-registration** of our filters, so they only run inside Spring Security. | JwtAuthenticationFilter, RateLimitFilter | Why disable CSRF for a JWT API? Why `STATELESS`? |
| `OpenApiConfig` | Swagger UI with a "Bearer" button | springdoc | — |
| `SchedulingConfig` | `@EnableScheduling`, can be switched off with `app.jobs.enabled=false` (tests) | — | How do you stop scheduled jobs in tests? |

## 2. Entities (`entity/`): map tables to Java

| Class | Key points |
|---|---|
| `Shelter` | `@Version` optimistic lock. Business methods `admit()`, `release()`, `freePlaces()`, `status()`, so the rules live with the data (rich domain model). |
| `OccupancyStatus` | Enum with a static `of(occupancy, capacity)`. Pure and fully unit-tested. |
| `Evacuee` | `@PrePersist/@PreUpdate` keeps the `name_normalized` / `village_normalized` search columns in sync. `checkOut()` refuses a second check-out. |
| `SupplyRequest` + `RequestLine` | Header and lines. `RequestLine` has a composite key (`@EmbeddedId` + `@MapsId`). The state machine methods (`approve`, `receive`, ...) throw `InvalidStateException`. |
| `ShelterStock` | Composite PK (shelter, item), `@Version`, `apply(delta)` refuses negative stock. |
| `StockMovement` | Append-only ledger. The DB unique key prevents double receipts. |
| `ShortageAlert` | The open/resolved lifecycle used by the idempotent job. |

Interview questions:
- Why `FetchType.LAZY`?
- What is the N+1 problem, and how does `join fetch` in `ShelterRepository.findWithDistrict` avoid it?
- Why `EnumType.STRING` instead of ORDINAL?

## 3. Repositories (`repository/`)

Spring Data interfaces. Custom JPQL with **optional filters**:

```
(:districtId is null or d.id = :districtId)
```

One query serves both STATE_ADMIN (null = all) and district users.

`EvacueeRepository.publicFind` uses a **constructor expression** (`new ...PublicMatch(s.name, d.name)`), so the public query can't even load private columns.

## 4. Security (`security/`)

| Class | What it does |
|---|---|
| `JwtService` | Issues and validates tokens (subject = user id, claims = role, district, jti). Returns `Optional.empty()` for invalid tokens instead of throwing. |
| `JwtAuthenticationFilter` | Reads `Authorization: Bearer`, checks the revoked list, reloads the user, sets the `SecurityContext`. |
| `AppUserPrincipal` | A record implementing `UserDetails`. It carries `districtId`, so services can scope queries without extra DB calls. |
| `AccessPolicy` | **All "may this user touch this record?" rules in one class.** Out-of-scope → 404. |
| `RateLimitFilter` | Fixed-window counter per IP for public and login URLs. Easy to explain; its limits are documented. |

Interview question: what is the difference between authentication and authorisation in this project? (JWT filter vs `@PreAuthorize` + `AccessPolicy`.)

## 5. Services (`service/`): the business logic

| Class | What it does | Interview question |
|---|---|---|
| `CapacityService` | Check-in/out with `TransactionTemplate` + retry on optimistic-lock failure. Adds nearest alternatives when the shelter is full. | Why can't you retry inside one `@Transactional` method? (The same persistence context would keep the stale entity.) |
| `NearestShelterService` | Bounding box in SQL, Haversine in Java, static `rank()` for unit tests | Why filter in SQL first? |
| `ShortageCalculator` / `ShortageService` | Pure formula + idempotent detection | What makes a job idempotent? |
| `SupplyService` | Workflow, history, stock receipt exactly once | Name the three guarantees against a double receipt. |
| `StockService` | One place that changes stock and writes the ledger | Why never update `shelter_stock` directly from other services? |
| `PublicService` | Privacy rules for public endpoints | How do you stop scraping of a people-finder? |
| `DashboardService` | Aggregates via `GROUP BY` queries | Why no hard-coded numbers? |
| `UserService` | No self-registration; district admins have limited powers | How do you stop privilege escalation? |

## 6. Controllers (`controller/`)

Thin: `@Valid` DTO, `@PreAuthorize`, call one service method, return a DTO.

Entities are never returned: that would cause lazy-loading errors and leak fields such as `password_hash`.

## 7. Exceptions (`exception/`)

`ApiException` subclasses carry the HTTP status and a stable `code` (e.g. `SHELTER_FULL`). `GlobalExceptionHandler` turns everything into one `ApiError` JSON. Unexpected errors are logged and the client sees only "Something went wrong".

## 8. Scheduler (`scheduler/`)

- `ShortageCheckJob`: hourly; catches errors so one failure doesn't stop future runs.
- `OccupancySnapshotJob`: hourly; skips rows that already exist for that hour.
- `TokenCleanupJob`: deletes expired revoked tokens.

## 9. Frontend (`frontend/src`)

| Part | What it does |
|---|---|
| `api/client.ts` | Axios instance. Adds the token, handles 401 globally, `errorMessage()` reads `ApiError`. |
| `api/useApi.ts` | Tiny fetch hook with loading, error and reload |
| `auth/AuthContext.tsx` | Login/logout state; role helpers |
| `components/RequireRole.tsx` | Route guard (convenience only; the backend enforces again) |
| `pages/shelter/*` | Check-in, evacuees, stock, supplies, nearest, facilities |
| `pages/admin/*` | Dashboards (Recharts), shelters, supply queue, events, users |
| `pages/public/*` | Family finder and available shelters, with privacy and demo notices |
