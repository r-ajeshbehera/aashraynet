# Shortage Calculation

```
days remaining = current stock quantity / (current occupancy × per-person daily consumption)
```

The code is in `ShortageCalculator.daysRemaining()`, a pure function:

- It returns *empty* when occupancy ≤ 0 or the rate ≤ 0, which avoids dividing by zero. Blankets have rate 0, so they are not consumption-tracked.
- Results are rounded **down** to 2 decimals, so 0.999 days still counts as a shortage.
- `isShortage(days)` is `days < 1`.
- A consumption-tracked item with **no stock row** at an occupied shelter counts as 0 (decision D5).
- Daily rates are stored in `supply_items.per_person_per_day`. The demo rates are rice 0.40 kg and water 4.00 L.

## Live list versus stored alerts

| | Live (`GET /api/shortages`) | Stored alerts (`GET /api/alerts`) |
|---|---|---|
| Source | calculated on request | `shortage_alerts` table |
| Updated by | — | `ShortageCheckJob` (hourly, cron `app.jobs.shortage-cron`) |

## Idempotent detection (`ShortageService.detect`)

For each current shortage:
- if an **open** alert exists for that shelter and item, refresh its `days_remaining` and `last_checked_at`
- otherwise insert a new alert

Any open alert that is no longer a shortage gets `resolved_at = now`.

Running the job twice creates nothing new. The DB unique key `(shelter_id, item_id, active_key)` (`active_key` = 1 while open) also blocks duplicates if two runs overlap. Failures are logged and the job continues at the next schedule.

## Example from the demo data (checked on MySQL 8)

Satapada has 59 people and 20 kg of rice: 20 / (59 × 0.40) = **0.84 days**, so it is a shortage.
