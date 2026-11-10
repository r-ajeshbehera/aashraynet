# AashrayNet — Database Design

Migration: `backend/src/main/resources/db/migration/V1__init_schema.sql` (Flyway).
Engine: InnoDB, `utf8mb4_0900_ai_ci`. **Validated on MySQL 8.0.46** (results in §4).

## 1. Tables

| Table | Purpose | Key rules |
|---|---|---|
| districts | District registry (Puri, Kendrapara) | unique name |
| users | Staff accounts | unique email; BCrypt hash; `CHECK` → STATE_ADMIN has no district, every other role must have one |
| disaster_events | Events like "Cyclone Dana 2026 (DEMO)" | `end_date >= start_date`; end date nullable while ongoing |
| shelters | Shelter registry and live counter | `capacity > 0`, `0 <= occupancy <= capacity`, lat/lng range, `version` for optimistic locking, `active` flag |
| evacuees | Check-in/out records | age 0–120, gender and special-need enums, `checked_out_at >= checked_in_at`; normalised name/village columns for the public finder |
| supply_items | Catalogue with per-person daily rate | rate ≥ 0 (0 = not consumption-tracked, e.g. blankets) |
| shelter_stock | Current quantity per shelter+item | **composite PK** (shelter_id, item_id); `quantity >= 0`; `version` |
| supply_requests | Request header | status enum; `CHECK`s make sure approver / dispatcher / receiver exist for each state; `version` |
| request_lines | Request lines | composite PK (request_id, item_id); `quantity > 0` |
| supply_request_history | Every status change | who, when, from → to |
| stock_movements | Every stock change (ledger) | `UNIQUE(request_id, item_id)` = a request line can update stock **only once**; `RECEIPT` ⇔ has request; `balance_after >= 0` |
| shortage_alerts | Shortage records | generated `active_key` + `UNIQUE(shelter_id, item_id, active_key)` = only one open alert |
| occupancy_snapshots | Optional hourly report data | unique per event+shelter+hour (idempotent) |
| revoked_tokens | JWT logout list | PK = token `jti` |
| audit_logs | Audit trail | never stores personal data |

## 2. Improvements over the conceptual SQL in the plan (deliberate, documented)

1. **`users.district_id` CHECK.** Ties district scoping to role at DB level.
2. **`shelters.active`.** Needed by the nearest-shelter filter ("exclude inactive shelters").
3. **`shelters.version` and `shelter_stock.version`.** Optimistic locking for check-ins and concurrent receipts.
4. **`evacuees.name_normalized` / `village_normalized`.**
   - The public finder uses exact *normalised* matching: lower-case, trimmed, single spaces.
   - The service fills these columns in `@PrePersist`/`@PreUpdate`.
   - The index is on `(name_normalized, village_normalized)`.
5. **`evacuees.checked_out_by`.** An audit field.
6. **`supply_requests`.**
   - Added `event_id`, so requests are event-specific.
   - Added actor and timestamp columns for every step, plus `rejection_reason` and `version`.
7. **New `stock_movements` ledger.**
   - Required by "movement history".
   - Its unique key is the DB-level guarantee that a receipt can never be applied twice.
8. **New `supply_request_history`.** Required by "complete supply-request history".
9. **New `shortage_alerts`.**
   - MySQL has no partial unique index.
   - Instead, a stored generated column is 1 while an alert is open and NULL once resolved.
   - A UNIQUE key treats NULLs as distinct, so resolved alerts don't block new ones.
10. **New `revoked_tokens` and `audit_logs`.** For logout and auditing.
11. **All FKs use RESTRICT.**
    - MySQL 8 rejects CHECK constraints on columns used by `ON DELETE CASCADE/SET NULL`.
    - Deleting operational records is also not wanted (deactivate instead).

## 3. Rules enforced by the application (not expressible as constraints)

These are covered by tests:
- **Occupancy = active evacuees.** `shelters.occupancy` is a counter updated in the *same transaction* as each check-in/check-out. A nightly check compares it with `COUNT(evacuees WHERE checked_out_at IS NULL)` and reports any drift.
- **Check-ins only in active events.** A check-in requires an active event. A checked-out evacuee no longer counts.
- **In-charge eligibility.** `shelters.incharge_id` must be a SHELTER_INCHARGE user of the same district.
- **District scoping.** Every query for DISTRICT_ADMIN / VOLUNTEER / SHELTER_INCHARGE includes the user's district.
- **Stock updates.** Changes go through `StockService` only: lock with `version`, write the ledger row, then update the quantity.

## 4. MySQL 8.0.46 validation results (actual run)

| # | Test | Result |
|---|---|---|
| 1 | STATE_ADMIN with a district | rejected (`ck_users_district_scope`) |
| 2 | occupancy 501 / capacity 500 | rejected (`ck_shelters_occupancy`) |
| 3 | occupancy -1 | rejected |
| 4 | capacity 0 | rejected (`ck_shelters_capacity`) |
| 5 | negative stock | rejected (`ck_stock_quantity`) |
| 6 | request line quantity 0 | rejected (`ck_rl_quantity`) |
| 7 | APPROVED without approver | rejected (`ck_sr_approved`) |
| 8 | RECEIVED without dispatcher | rejected (`ck_sr_dispatched`) |
| 9 | valid REQUESTED→RECEIVED with all actors | accepted |
| 10 | first receipt movement | accepted |
| 11 | **same receipt applied again** | **rejected — duplicate key `uq_sm_receipt_once`** |
| 12 | RECEIPT movement without request | rejected (`ck_sm_receipt_link`) |
| 13 | receipt for an item not in the request | rejected (FK `fk_sm_line`) |
| 14 | first open shortage alert | accepted |
| 15 | **second open alert, same shelter+item** | **rejected — duplicate key `uq_sa_one_open`** |
| 16 | resolve, then open a new alert | accepted (`active_key` NULL / 1) |
| 17 | check-out before check-in | rejected (`ck_evacuees_times`) |
| 18 | invalid special-need value | rejected (`ck_evacuees_need`) |

Shortage query on the seed data (occupancy 499; Rice 150 kg at 0.40 kg/day; Water 1500 L at 4 L/day) gave 0.75 days for both items, which is correct.

## 5. Indexes

| Index | Used by |
|---|---|
| `shelters(district_id, active)` | district lists, nearest-shelter candidates |
| `shelters(incharge_id)` | "My Shelter" |
| `evacuees(name_normalized, village_normalized)` | public family finder |
| `evacuees(shelter_id, checked_out_at)` | active evacuee list, occupancy reconciliation |
| `evacuees(event_id, shelter_id)` | event reports |
| `supply_requests(shelter_id, status)`, `(event_id, status)` | request queues |
| `stock_movements(shelter_id, item_id, created_at)` | movement history |
| `shortage_alerts(event_id, resolved_at)` | shortage dashboard |
