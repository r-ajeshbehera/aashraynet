# Interview Preparation: AashrayNet

## 60-second pitch

"AashrayNet is a cyclone shelter and relief system for Odisha, built with Java 21, Spring Boot, Spring Security JWT, JPA/Hibernate, MySQL with Flyway, and React.
- Volunteers check evacuees into shelters, and the shelter's status (green, yellow, red, full) updates live.
- If a shelter is full, the API suggests the nearest shelters with enough free places, using the Haversine formula.
- In-charges request supplies through an approve, dispatch, receive workflow, and stock is updated exactly once.
- An hourly job flags any item that will run out within a day.

The hardest part was concurrency: two volunteers taking the last bed at the same time. I solved it with JPA optimistic locking plus a retry, and a database CHECK constraint as the final safety net. I proved it with a two-thread test."

## Deep-dive answers

**1. Capacity locking**
- `Shelter` has `@Version`. Hibernate's UPDATE includes `WHERE version = ?`.
- If another transaction committed first, 0 rows are updated and Spring throws `ObjectOptimisticLockingFailureException`.
- I retry up to 3 times, each in a **new** transaction (`TransactionTemplate`), so the retry reads fresh data. It then usually sees the shelter is full and returns 422 with alternatives.
- The DB `CHECK (occupancy <= capacity)` guarantees correctness even if the code had a bug.
- *Follow-up: why not pessimistic locking (`SELECT ... FOR UPDATE`)?* Conflicts are rare, so optimistic locking avoids holding row locks. Pessimistic locking is also valid under heavy contention.

**2. Haversine**
- It gives the great-circle distance on a sphere: `a = sin²(Δφ/2) + cos φ1 cos φ2 sin²(Δλ/2)`, `d = 2R·atan2(√a, √(1−a))`, with R = 6371 km.
- I first filter in SQL with a bounding box (cheap BETWEEN on latitude/longitude) and the capacity rule, then compute the exact distance in Java, sort, and limit.
- Accuracy is about 0.5 %, which is fine for "nearest shelter". Road distance would need a routing API.

**3. Stock calculations**
- `days = stock / (occupancy × daily rate)` with BigDecimal (never double for quantities), rounded down.
- Zero occupancy or a zero rate gives "not calculable", which avoids dividing by zero.
- Alerts are idempotent: one open alert per shelter and item, enforced by a unique key on a generated column, because MySQL has no partial indexes.

**4. District-level security**
- The JWT carries role and district, and the filter reloads the user each request.
- `@PreAuthorize` checks the role. `AccessPolicy` checks the record's district or in-charge.
- Repository queries include the district, so data from another district is never even loaded.
- Out-of-scope returns 404, not 403, so attackers can't discover ids.
- Tested with MockMvc: the Puri admin gets 404 for a Kendrapara shelter.

**5. Receipt exactly once.** Three layers: the state machine, `@Version` on the request, and `UNIQUE(request_id, item_id)` on the stock ledger.

**6. Public finder privacy.**
- A dedicated DTO with only shelter name and district.
- Exact normalised matching, so the list can't be browsed.
- The same response when nothing is found.
- A rate limit.

## Likely questions

- Why Flyway and not `ddl-auto=update`? (Versioned, reviewable, repeatable migrations.)
- How do you avoid N+1 in the shelter list? (`join fetch` district.)
- What would change for 10 districts and 2 000 shelters? (Spatial index, Redis rate limiting, caching of public pages, read replicas.)
- What is not done yet, and why? (Be honest: see README "Known limitations".)
