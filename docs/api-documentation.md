# AashrayNet — REST API Specification (Phase 1, for review)

Base path `/api`. JSON in and out. Auth uses `Authorization: Bearer <jwt>`. Lists take `page`, `size` (max 100) and `sort`.

**Roles:** VOL = VOLUNTEER, INC = SHELTER_INCHARGE, DA = DISTRICT_ADMIN, SA = STATE_ADMIN, PUB = no login.
"(own)" means the server limits results to the caller's district or shelter.

## Authentication and users

| Method | Path | Roles | Notes |
|---|---|---|---|
| POST | `/auth/login` | PUB | returns `{accessToken, expiresIn, user{id, name, role, districtId}}`; rate-limited |
| POST | `/auth/logout` | any | revokes the token `jti` |
| GET | `/auth/me` | any | current profile |
| POST | `/users` | SA, DA (own) | create staff; DA can create only VOL/INC in own district; nobody can self-register |
| GET | `/users` | SA, DA (own) | filter by role, district |
| PATCH | `/users/{id}` | SA, DA (own) | name, active, role (within limits) |
| PATCH | `/users/me/password` | any | change own password |

## Events and districts

| Method | Path | Roles | Notes |
|---|---|---|---|
| POST | `/events` | SA | `{name, startDate, endDate?}` |
| GET | `/events` | any authenticated | `?active=true` |
| GET | `/events/{id}` | any authenticated | |
| PATCH | `/events/{id}` | SA | edit, end, deactivate |
| GET | `/districts` | any authenticated + PUB list | id, name |

## Shelters and occupancy

| Method | Path | Roles | Notes |
|---|---|---|---|
| POST | `/shelters` | SA, DA (own) | create |
| GET | `/shelters` | SA, DA (own), VOL (own), INC (own) | `?district=1&status=GREEN&active=true`; each item has capacity, occupancy, freePlaces, occupancyPercent, status |
| GET | `/shelters/{id}` | same scope | details plus facilities |
| PUT | `/shelters/{id}` | SA, DA (own) | edit core fields (capacity can't drop below occupancy) |
| PATCH | `/shelters/{id}/facilities` | DA (own), INC (own) | `{hasMedical, hasRamp, hasGenerator}` |
| PATCH | `/shelters/{id}/incharge` | DA (own) | `{userId}` |
| GET | `/shelters/{id}/nearest-available?count=3&people=1&radiusKm=50` | VOL, INC, DA, SA | Haversine-sorted list |

## Evacuees

| Method | Path | Roles | Notes |
|---|---|---|---|
| POST | `/shelters/{id}/evacuees` | VOL (own district), INC (own) | check-in. **201**; **422 SHELTER_FULL** with `alternatives[]`; **409 CONCURRENT_UPDATE** after retries |
| GET | `/shelters/{id}/evacuees` | INC (own), DA (own), SA; VOL sees no special-need field | `?active=true&q=name` |
| POST | `/evacuees/{id}/checkout` | INC (own), DA (own) | 200; 409 if already checked out |
| GET | `/evacuees/{id}` | INC (own), DA (own), SA | |

## Supplies and stock

| Method | Path | Roles | Notes |
|---|---|---|---|
| GET/POST | `/supply-items` | GET any authenticated; POST SA | catalogue with `perPersonPerDay` |
| GET | `/shelters/{id}/stock` | INC (own), DA (own), SA | quantity plus days remaining |
| POST | `/shelters/{id}/stock/adjustments` | INC (own), DA (own) | `{itemId, type: INITIAL / CONSUMPTION / ADJUSTMENT_IN / ADJUSTMENT_OUT, quantity, reason}`; negative result → 422 |
| GET | `/shelters/{id}/stock/movements` | INC (own), DA (own), SA | ledger, paged |
| POST | `/shelters/{id}/supply-requests` | INC (own) | `{lines:[{itemId, quantity}], note}` → REQUESTED |
| GET | `/supply-requests` | INC (own), DA (own), SA | `?status=REQUESTED&shelter=` |
| GET | `/supply-requests/{id}` | same | includes lines and history |
| POST | `/supply-requests/{id}/approve` | DA (own) | REQUESTED → APPROVED |
| POST | `/supply-requests/{id}/reject` | DA (own) | `{reason}` REQUESTED → REJECTED |
| POST | `/supply-requests/{id}/dispatch` | DA (own) | APPROVED → DISPATCHED |
| POST | `/supply-requests/{id}/receive` | INC (own) | DISPATCHED → RECEIVED; stock updated once; a repeat call → **409** |

## Shortages and alerts

| Method | Path | Roles | Notes |
|---|---|---|---|
| GET | `/shortages?district=1` | DA (own), SA, INC (own shelter only) | live calculation: shelter, item, quantity, occupancy, daysRemaining |
| GET | `/alerts?status=OPEN` | DA (own), SA, INC (own) | stored alert records |

## Public (no login, rate-limited, demo-data banner)

| Method | Path | Notes |
|---|---|---|
| GET | `/public/find?name=&village=` | returns `{matches:[{shelterName, district}], notice}` (max 5, distinct). Same response shape and 200 status whether or not anything matches. Only active check-ins in active events. Never returns age, gender, need, ids. 10 requests per minute per IP. |
| GET | `/public/shelters/available?district=1` | `[{shelterName, district, village, status, freePlaces, hasMedical, hasRamp, lastUpdated}]`, plus `dataNotice: "Demonstration data"` |

## Dashboards

| Method | Path | Roles | Notes |
|---|---|---|---|
| GET | `/dashboard/district/{id}?event=&from=&to=&shelter=` | DA (own), SA | totals, status counts, capacity, special-needs counts, pending requests, shortages, requests by status |
| GET | `/dashboard/state?event=` | SA | per-district summary |

## Error format (all errors)

```json
{ "timestamp": "2026-10-24T10:15:30Z", "status": 422, "code": "SHELTER_FULL",
  "message": "Shelter has no free places", "path": "/api/shelters/1/evacuees",
  "details": { "alternatives": [ { "shelterId": 3, "name": "Krushnaprasad MCS", "distanceKm": 7.0, "freePlaces": 240, "status": "GREEN" } ] } }
```

| Status | Meaning |
|---|---|
| 400 | validation error, with field errors |
| 401 | no or invalid token |
| 403 | wrong role |
| 404 | not found, *or outside your district* |
| 409 | conflict / invalid state transition |
| 422 | business rule broken |
| 429 | rate limit |
