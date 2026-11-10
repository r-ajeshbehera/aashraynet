# Supply Request Workflow

```
REQUESTED ──approve──► APPROVED ──dispatch──► DISPATCHED ──receive──► RECEIVED
    └──reject(reason)──► REJECTED
```

| Step | Who | Endpoint |
|---|---|---|
| create (header + lines) | SHELTER_INCHARGE of that shelter | `POST /api/shelters/{id}/supply-requests` |
| approve / reject | DISTRICT_ADMIN of that district | `POST /api/supply-requests/{id}/approve` · `/reject` |
| dispatch | DISTRICT_ADMIN | `POST /api/supply-requests/{id}/dispatch` |
| receive | SHELTER_INCHARGE | `POST /api/supply-requests/{id}/receive` |

The state machine lives in the `RequestStatus.allowedNext()` and `SupplyRequest.approve/reject/dispatch/receive` methods. An invalid move throws `InvalidStateException`, which returns **409**. Each move writes a `supply_request_history` row and an audit row.

## Receipt updates stock exactly once

In one transaction:

1. `request.receive()` fails unless the status is DISPATCHED.
2. `saveAndFlush(request)` runs the `@Version` check. Of two simultaneous clicks, one gets 409.
3. For each line, `StockService.addReceipt()`:
   - locks the `shelter_stock` row via its version
   - adds the quantity
   - inserts a `stock_movements` row of type RECEIPT with `request_id`
4. `UNIQUE(request_id, item_id)` on `stock_movements` makes the **database** refuse a second receipt row for the same line. This was tested on MySQL 8.

## Validation

- Quantities must be > 0 (Bean Validation and a DB CHECK).
- No duplicate items in one request.
- Only active items are accepted.
- The event must be active.
- Requests are scoped to the caller's district.

## Tests

- 10 state-machine unit tests (run)
- 2 Mockito tests (stock is updated exactly once per line; a second receipt never touches stock)
- the full HTTP workflow in `FullWorkflowIT`
