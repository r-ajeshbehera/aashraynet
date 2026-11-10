# AashrayNet — ER Diagram

Source of truth: `backend/src/main/resources/db/migration/V1__init_schema.sql`.
Audit fields (`created_at`, `updated_at`) are left out of the diagram to keep it readable.

```mermaid
erDiagram
    DISTRICTS ||--o{ USERS : "scopes"
    DISTRICTS ||--o{ SHELTERS : "has"
    USERS ||--o{ SHELTERS : "in-charge of"
    USERS ||--o{ DISASTER_EVENTS : "creates"
    DISASTER_EVENTS ||--o{ EVACUEES : "groups"
    SHELTERS ||--o{ EVACUEES : "houses"
    USERS ||--o{ EVACUEES : "registers"
    SHELTERS ||--o{ SHELTER_STOCK : "holds"
    SUPPLY_ITEMS ||--o{ SHELTER_STOCK : "stocked as"
    SHELTER_STOCK ||--o{ STOCK_MOVEMENTS : "changed by"
    DISASTER_EVENTS ||--o{ SUPPLY_REQUESTS : "groups"
    SHELTERS ||--o{ SUPPLY_REQUESTS : "raises"
    SUPPLY_REQUESTS ||--|{ REQUEST_LINES : "contains"
    SUPPLY_ITEMS ||--o{ REQUEST_LINES : "requested"
    REQUEST_LINES ||--o| STOCK_MOVEMENTS : "received once"
    SUPPLY_REQUESTS ||--o{ SUPPLY_REQUEST_HISTORY : "history"
    SHELTERS ||--o{ SHORTAGE_ALERTS : "alerts"
    SUPPLY_ITEMS ||--o{ SHORTAGE_ALERTS : "short of"
    DISASTER_EVENTS ||--o{ SHORTAGE_ALERTS : "during"
    SHELTERS ||--o{ OCCUPANCY_SNAPSHOTS : "snapshots"

    DISTRICTS { bigint id PK
        varchar name UK }
    USERS { bigint id PK
        varchar full_name
        varchar email UK
        char password_hash
        varchar role
        bigint district_id FK "NULL only for STATE_ADMIN"
        boolean active }
    DISASTER_EVENTS { bigint id PK
        varchar name UK
        date start_date
        date end_date
        boolean active }
    SHELTERS { bigint id PK
        bigint district_id FK
        varchar name
        varchar village
        decimal latitude
        decimal longitude
        int capacity "CHECK > 0"
        int occupancy "CHECK 0..capacity"
        boolean has_medical
        boolean has_ramp
        boolean has_generator
        bigint incharge_id FK
        boolean active
        bigint version "optimistic lock" }
    EVACUEES { bigint id PK
        bigint event_id FK
        bigint shelter_id FK
        varchar full_name
        varchar name_normalized
        smallint age
        varchar gender
        varchar village
        varchar village_normalized
        varchar special_need
        datetime checked_in_at
        datetime checked_out_at "NULL = still inside"
        bigint registered_by FK }
    SUPPLY_ITEMS { bigint id PK
        varchar name UK
        varchar unit
        decimal per_person_per_day }
    SHELTER_STOCK { bigint shelter_id PK,FK
        bigint item_id PK,FK
        decimal quantity "CHECK >= 0"
        bigint version }
    STOCK_MOVEMENTS { bigint id PK
        bigint shelter_id FK
        bigint item_id FK
        bigint event_id FK
        varchar movement_type
        decimal quantity_delta
        decimal balance_after
        bigint request_id FK "UNIQUE with item_id" }
    SUPPLY_REQUESTS { bigint id PK
        bigint event_id FK
        bigint shelter_id FK
        varchar status
        bigint requested_by FK
        bigint approved_by FK
        bigint dispatched_by FK
        bigint received_by FK
        datetime created_at
        bigint version }
    REQUEST_LINES { bigint request_id PK,FK
        bigint item_id PK,FK
        decimal quantity "CHECK > 0" }
    SUPPLY_REQUEST_HISTORY { bigint id PK
        bigint request_id FK
        varchar from_status
        varchar to_status
        bigint changed_by FK }
    SHORTAGE_ALERTS { bigint id PK
        bigint event_id FK
        bigint shelter_id FK
        bigint item_id FK
        decimal days_remaining
        datetime resolved_at
        tinyint active_key "generated; UNIQUE" }
    OCCUPANCY_SNAPSHOTS { bigint id PK
        bigint event_id FK
        bigint shelter_id FK
        int occupancy
        datetime taken_at }
```

Supporting tables not drawn: `revoked_tokens` (JWT logout) and `audit_logs`.
