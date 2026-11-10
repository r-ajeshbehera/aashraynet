# Occupancy and Capacity

## Status rules (`OccupancyStatus.of`)

| Condition | Status |
|---|---|
| occupancy == capacity | FULL |
| occupancy × 100 < 70 × capacity | GREEN |
| occupancy × 100 ≤ 90 × capacity | YELLOW |
| otherwise (above 90 %, not full) | RED |

Integer maths is used instead of `occupancy / capacity * 100` so that, for example, 7/10 is exactly 70 % and gives YELLOW. A `double` could give 69.99999 and wrongly return GREEN.

The API always returns `capacity`, `occupancy`, `freePlaces`, `occupancyPercent` (one decimal) and `status`.

## Check-in flow (`CapacityService.checkIn`)

```
withRetry (max 3 attempts, each in a NEW transaction):
    shelter = SELECT ... FROM shelters WHERE id = ?          (version = v)
    check access, shelter active, event active
    shelter.admit(1)          -> throws CapacityExceededException if freePlaces < 1
    INSERT evacuee
    UPDATE shelters SET occupancy = occupancy', version = v+1 WHERE id = ? AND version = v
       0 rows updated -> ObjectOptimisticLockingFailureException -> rollback -> retry
on CapacityExceededException: attach the 3 nearest alternatives -> HTTP 422 SHELTER_FULL
after 3 conflicts: HTTP 409 CONCURRENT_UPDATE
```

Check-out is the mirror image: it sets `checked_out_at` and calls `shelter.release(1)`, inside the same retry wrapper.

## Why overbooking is impossible

1. **Business rule:** `Shelter.admit()` refuses when there are not enough free places.
2. **Optimistic lock:** with `@Version`, two transactions that both read version *v* cannot both write. The second UPDATE matches 0 rows, so it fails and is retried with fresh data. On the retry it sees the shelter is full and returns 422.
3. **Database:** `CHECK (occupancy >= 0 AND occupancy <= capacity)` is the last safety net. This was tested on MySQL 8 (see database-design.md).

The concurrency test (`ConcurrentCheckInIT`) works like this:
- Satapada has 1 free place.
- Two threads are released at the same moment with a `CountDownLatch`.
- Expected statuses are {201, 422}, and final occupancy must equal capacity.

## Occupancy versus evacuee rows

`shelters.occupancy` is a counter. It always changes in the **same transaction** as the evacuee row it belongs to, so it equals `COUNT(evacuees WHERE checked_out_at IS NULL)`. A checked-out evacuee never counts.

## Alternative design (interview talking point)

`UPDATE shelters SET occupancy = occupancy + 1 WHERE id = ? AND occupancy < capacity` is also safe and needs no retry. It is an atomic conditional update. The spec asked for JPA `@Version`, which also protects every other shelter field from lost updates.
