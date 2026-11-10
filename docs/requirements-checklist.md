# AashrayNet — Requirements Checklist

Legend: ✅ done & verified · ⬜ not done

**Phase: implemented and verified. `mvn verify` BUILD SUCCESS, 68/68 tests, coverage 96.7 % (service layer).**

| # | Requirement | Code | Verification |
|---|---|---|---|
| 1 | Auth: login, JWT, BCrypt, roles, district scoping, logout (revocation), user management | ✅ | ✅ integration tests pass |
| 2 | Disaster events | ✅ | ✅ |
| 3 | Districts + scoping in queries | ✅ | ✅ (`districtAdminCannotSeeAnotherDistrictsShelter`) |
| 4 | Shelter registry, constraints, facilities, in-charge, `@Version` | ✅ | ✅ DB constraints on MySQL 8 · ✅ entity unit tests |
| 5 | Occupancy %, GREEN/YELLOW/RED/FULL (69/70/90/91/100) | ✅ | ✅ 8 unit tests run |
| 6 | Check-in / check-out / search / history | ✅ | ✅ capacity unit tests · ✅ API tests |
| 7 | Concurrency-safe capacity | ✅ | ✅ DB CHECK verified · ✅ two-thread test (needs Docker + Maven) |
| 8 | Nearest available shelter | ✅ | ✅ 12 unit tests run |
| 9 | Supply catalogue (rates in DB) | ✅ | ✅ demo data on MySQL 8 |
| 10 | Shelter stock + movement ledger | ✅ | ✅ DB tests · ✅ unit |
| 11 | Supply workflow incl. REJECTED, exactly-once receipt | ✅ | ✅ 10 state-machine tests · ✅ DB unique key · ✅ Mockito/API |
| 12 | Shortage detection (idempotent, hourly) | ✅ | ✅ 7 calculator tests · ✅ DB unique key · ✅ Mockito |
| 13 | Public family finder (privacy, rate limit, notice) | ✅ | ✅ API test passes |
| 14 | Public shelter availability (demo label, timestamp) | ✅ | ✅ |
| 15 | District dashboard | ✅ | ✅ |
| 16 | State dashboard | ✅ | ✅ |
| 17 | Scheduled jobs (shortage, snapshots, token cleanup) | ✅ | ✅ |
| — | React frontend, all roles | ✅ | ✅ lint + build · ✅ used in the browser against the running stack (screenshots) |
| — | Meaningful tests | ✅ 68 | ✅ 68/68 pass (incl. concurrency, 5/5 repeats) |
| — | Docker, Compose, Nginx, GitHub Actions | ✅ | ✅ compose stack ran healthy (runtime images); CI workflow ready |
| — | Postman collection (29 requests) | ✅ | ⬜ not run |
| — | Swagger/OpenAPI | ✅ | ✅ |
| — | Documentation (14 docs + README) | ✅ | ✅ |
| — | Screenshots | ✅ | ✅ from the running app (`docs/screenshots/`) |
| — | Live deployment | ⬜ | not deployed; no URL claimed |
| N1–N6 | Nice-to-have (map, Odia/Hindi, offline, PDF export, roster) | ⬜ | after MVP is verified (CSV export of shelters is included) |

## Decisions (approved by you: "do complete")
D1 Spring Boot 3.5.x · D2 volunteers district-wide · D3 cross-district suggestions within radius · D4 up to 5 finder results · D5 missing stock row = 0 · D6 TypeScript
