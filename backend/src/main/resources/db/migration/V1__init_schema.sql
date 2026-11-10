-- =====================================================================
-- AashrayNet - V1 initial schema (MySQL 8, InnoDB, utf8mb4)
-- Validated on MySQL 8.0.46. All FKs use the default RESTRICT action
-- because MySQL forbids CHECK constraints on columns used by
-- CASCADE / SET NULL foreign keys.
-- =====================================================================

CREATE TABLE districts (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(80)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_districts PRIMARY KEY (id),
    CONSTRAINT uq_districts_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE users (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    full_name      VARCHAR(120) NOT NULL,
    email          VARCHAR(160) NOT NULL,
    password_hash  CHAR(60)     NOT NULL,              -- BCrypt
    role           VARCHAR(20)  NOT NULL,
    district_id    BIGINT       NULL,                  -- NULL only for STATE_ADMIN
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT fk_users_district FOREIGN KEY (district_id) REFERENCES districts (id),
    CONSTRAINT ck_users_role CHECK (role IN ('VOLUNTEER','SHELTER_INCHARGE','DISTRICT_ADMIN','STATE_ADMIN')),
    CONSTRAINT ck_users_district_scope CHECK (
        (role = 'STATE_ADMIN' AND district_id IS NULL) OR
        (role <> 'STATE_ADMIN' AND district_id IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE disaster_events (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(120) NOT NULL,
    start_date  DATE         NOT NULL,
    end_date    DATE         NULL,                     -- NULL while the event is ongoing
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by  BIGINT       NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_disaster_events PRIMARY KEY (id),
    CONSTRAINT uq_disaster_events_name UNIQUE (name),
    CONSTRAINT fk_events_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT ck_events_dates CHECK (end_date IS NULL OR end_date >= start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE shelters (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    district_id    BIGINT        NOT NULL,
    name           VARCHAR(120)  NOT NULL,
    village        VARCHAR(120)  NOT NULL,
    latitude       DECIMAL(9,6)  NOT NULL,
    longitude      DECIMAL(9,6)  NOT NULL,
    capacity       INT           NOT NULL,
    occupancy      INT           NOT NULL DEFAULT 0,
    has_medical    BOOLEAN       NOT NULL DEFAULT FALSE,
    has_ramp       BOOLEAN       NOT NULL DEFAULT FALSE,
    has_generator  BOOLEAN       NOT NULL DEFAULT FALSE,
    incharge_id    BIGINT        NULL,
    active         BOOLEAN       NOT NULL DEFAULT TRUE,
    version        BIGINT        NOT NULL DEFAULT 0,  -- JPA @Version (optimistic lock)
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_shelters PRIMARY KEY (id),
    CONSTRAINT uq_shelters_district_name UNIQUE (district_id, name),
    CONSTRAINT fk_shelters_district FOREIGN KEY (district_id) REFERENCES districts (id),
    CONSTRAINT fk_shelters_incharge FOREIGN KEY (incharge_id) REFERENCES users (id),
    CONSTRAINT ck_shelters_capacity  CHECK (capacity > 0),
    CONSTRAINT ck_shelters_occupancy CHECK (occupancy >= 0 AND occupancy <= capacity),
    CONSTRAINT ck_shelters_lat CHECK (latitude  BETWEEN -90  AND 90),
    CONSTRAINT ck_shelters_lng CHECK (longitude BETWEEN -180 AND 180)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_shelters_district ON shelters (district_id, active);
CREATE INDEX idx_shelters_incharge ON shelters (incharge_id);

CREATE TABLE evacuees (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    event_id            BIGINT       NOT NULL,
    shelter_id          BIGINT       NOT NULL,
    full_name           VARCHAR(120) NOT NULL,
    name_normalized     VARCHAR(120) NOT NULL,  -- lower-case, trimmed, single spaces (set by app)
    age                 SMALLINT     NOT NULL,
    gender              VARCHAR(10)  NOT NULL,
    village             VARCHAR(120) NOT NULL,
    village_normalized  VARCHAR(120) NOT NULL,
    special_need        VARCHAR(10)  NOT NULL DEFAULT 'NONE',
    checked_in_at       DATETIME(6)  NOT NULL,
    checked_out_at      DATETIME(6)  NULL,      -- NULL = currently in the shelter
    registered_by       BIGINT       NOT NULL,
    checked_out_by      BIGINT       NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_evacuees PRIMARY KEY (id),
    CONSTRAINT fk_evacuees_event      FOREIGN KEY (event_id)       REFERENCES disaster_events (id),
    CONSTRAINT fk_evacuees_shelter    FOREIGN KEY (shelter_id)     REFERENCES shelters (id),
    CONSTRAINT fk_evacuees_registered FOREIGN KEY (registered_by)  REFERENCES users (id),
    CONSTRAINT fk_evacuees_checkedout FOREIGN KEY (checked_out_by) REFERENCES users (id),
    CONSTRAINT ck_evacuees_age    CHECK (age BETWEEN 0 AND 120),
    CONSTRAINT ck_evacuees_gender CHECK (gender IN ('MALE','FEMALE','OTHER')),
    CONSTRAINT ck_evacuees_need   CHECK (special_need IN ('NONE','PREGNANT','ELDERLY','DISABLED','INFANT','MEDICAL')),
    CONSTRAINT ck_evacuees_times  CHECK (checked_out_at IS NULL OR checked_out_at >= checked_in_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_evacuees_name_village   ON evacuees (name_normalized, village_normalized);
CREATE INDEX idx_evacuees_shelter_active ON evacuees (shelter_id, checked_out_at);
CREATE INDEX idx_evacuees_event          ON evacuees (event_id, shelter_id);

CREATE TABLE supply_items (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    name                VARCHAR(80)   NOT NULL,
    unit                VARCHAR(20)   NOT NULL,
    per_person_per_day  DECIMAL(8,3)  NOT NULL DEFAULT 0,  -- 0 = not consumption-tracked (e.g. blankets)
    active              BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_supply_items PRIMARY KEY (id),
    CONSTRAINT uq_supply_items_name UNIQUE (name),
    CONSTRAINT ck_supply_items_rate CHECK (per_person_per_day >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE shelter_stock (
    shelter_id  BIGINT         NOT NULL,
    item_id     BIGINT         NOT NULL,
    quantity    DECIMAL(12,2)  NOT NULL DEFAULT 0,
    version     BIGINT         NOT NULL DEFAULT 0,   -- protects concurrent receipts/consumption
    updated_at  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_shelter_stock PRIMARY KEY (shelter_id, item_id),
    CONSTRAINT fk_stock_shelter FOREIGN KEY (shelter_id) REFERENCES shelters (id),
    CONSTRAINT fk_stock_item    FOREIGN KEY (item_id)    REFERENCES supply_items (id),
    CONSTRAINT ck_stock_quantity CHECK (quantity >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE supply_requests (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    event_id          BIGINT       NOT NULL,
    shelter_id        BIGINT       NOT NULL,
    status            VARCHAR(12)  NOT NULL DEFAULT 'REQUESTED',
    requested_by      BIGINT       NOT NULL,
    approved_by       BIGINT       NULL,       -- also stores the rejecting admin
    dispatched_by     BIGINT       NULL,
    received_by       BIGINT       NULL,
    rejection_reason  VARCHAR(255) NULL,
    note              VARCHAR(255) NULL,
    created_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    decided_at        DATETIME(6)  NULL,
    dispatched_at     DATETIME(6)  NULL,
    received_at       DATETIME(6)  NULL,
    version           BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_supply_requests PRIMARY KEY (id),
    CONSTRAINT fk_sr_event      FOREIGN KEY (event_id)      REFERENCES disaster_events (id),
    CONSTRAINT fk_sr_shelter    FOREIGN KEY (shelter_id)    REFERENCES shelters (id),
    CONSTRAINT fk_sr_requested  FOREIGN KEY (requested_by)  REFERENCES users (id),
    CONSTRAINT fk_sr_approved   FOREIGN KEY (approved_by)   REFERENCES users (id),
    CONSTRAINT fk_sr_dispatched FOREIGN KEY (dispatched_by) REFERENCES users (id),
    CONSTRAINT fk_sr_received   FOREIGN KEY (received_by)   REFERENCES users (id),
    CONSTRAINT ck_sr_status CHECK (status IN ('REQUESTED','APPROVED','REJECTED','DISPATCHED','RECEIVED')),
    -- a state can only exist once the actor of the previous step is recorded
    CONSTRAINT ck_sr_approved   CHECK (status IN ('REQUESTED') OR approved_by IS NOT NULL),
    CONSTRAINT ck_sr_dispatched CHECK (status NOT IN ('DISPATCHED','RECEIVED') OR dispatched_by IS NOT NULL),
    CONSTRAINT ck_sr_received   CHECK (status <> 'RECEIVED' OR received_by IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_sr_shelter_status ON supply_requests (shelter_id, status);
CREATE INDEX idx_sr_event_status   ON supply_requests (event_id, status);

CREATE TABLE request_lines (
    request_id  BIGINT         NOT NULL,
    item_id     BIGINT         NOT NULL,
    quantity    DECIMAL(12,2)  NOT NULL,
    CONSTRAINT pk_request_lines PRIMARY KEY (request_id, item_id),
    CONSTRAINT fk_rl_request FOREIGN KEY (request_id) REFERENCES supply_requests (id),
    CONSTRAINT fk_rl_item    FOREIGN KEY (item_id)    REFERENCES supply_items (id),
    CONSTRAINT ck_rl_quantity CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Full status history of every supply request (who moved it, when)
CREATE TABLE supply_request_history (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    request_id   BIGINT       NOT NULL,
    from_status  VARCHAR(12)  NULL,
    to_status    VARCHAR(12)  NOT NULL,
    changed_by   BIGINT       NOT NULL,
    note         VARCHAR(255) NULL,
    changed_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_srh PRIMARY KEY (id),
    CONSTRAINT fk_srh_request FOREIGN KEY (request_id) REFERENCES supply_requests (id),
    CONSTRAINT fk_srh_user    FOREIGN KEY (changed_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_srh_request ON supply_request_history (request_id, changed_at);

-- Every stock change. A receipt points to its request line; the UNIQUE key
-- makes it impossible to apply the same request line to stock twice.
CREATE TABLE stock_movements (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    shelter_id      BIGINT         NOT NULL,
    item_id         BIGINT         NOT NULL,
    event_id        BIGINT         NULL,      -- NULL = pre-positioned stock before any event
    movement_type   VARCHAR(16)    NOT NULL,
    quantity_delta  DECIMAL(12,2)  NOT NULL,  -- + in, - out
    balance_after   DECIMAL(12,2)  NOT NULL,
    request_id      BIGINT         NULL,
    reason          VARCHAR(255)   NULL,
    performed_by    BIGINT         NOT NULL,
    created_at      DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_stock_movements PRIMARY KEY (id),
    CONSTRAINT fk_sm_stock   FOREIGN KEY (shelter_id, item_id) REFERENCES shelter_stock (shelter_id, item_id),
    CONSTRAINT fk_sm_event   FOREIGN KEY (event_id)     REFERENCES disaster_events (id),
    CONSTRAINT fk_sm_line    FOREIGN KEY (request_id, item_id) REFERENCES request_lines (request_id, item_id),
    CONSTRAINT fk_sm_user    FOREIGN KEY (performed_by) REFERENCES users (id),
    CONSTRAINT uq_sm_receipt_once UNIQUE (request_id, item_id),
    CONSTRAINT ck_sm_type CHECK (movement_type IN ('INITIAL','RECEIPT','CONSUMPTION','ADJUSTMENT_IN','ADJUSTMENT_OUT')),
    CONSTRAINT ck_sm_delta CHECK (quantity_delta <> 0),
    CONSTRAINT ck_sm_balance CHECK (balance_after >= 0),
    CONSTRAINT ck_sm_receipt_link CHECK ((movement_type = 'RECEIPT') = (request_id IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_sm_stock_time ON stock_movements (shelter_id, item_id, created_at);

-- Shortage alerts. active_key is 1 while unresolved and NULL after;
-- UNIQUE(shelter, item, active_key) => at most ONE open alert per shelter+item,
-- so the hourly job is idempotent even if it runs twice.
CREATE TABLE shortage_alerts (
    id                      BIGINT         NOT NULL AUTO_INCREMENT,
    event_id                BIGINT         NOT NULL,
    shelter_id              BIGINT         NOT NULL,
    item_id                 BIGINT         NOT NULL,
    days_remaining          DECIMAL(8,2)   NOT NULL,
    quantity_at_detection   DECIMAL(12,2)  NOT NULL,
    occupancy_at_detection  INT            NOT NULL,
    detected_at             DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_checked_at         DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    resolved_at             DATETIME(6)    NULL,
    active_key              TINYINT AS (IF(resolved_at IS NULL, 1, NULL)) STORED,
    CONSTRAINT pk_shortage_alerts PRIMARY KEY (id),
    CONSTRAINT fk_sa_event   FOREIGN KEY (event_id)   REFERENCES disaster_events (id),
    CONSTRAINT fk_sa_shelter FOREIGN KEY (shelter_id) REFERENCES shelters (id),
    CONSTRAINT fk_sa_item    FOREIGN KEY (item_id)    REFERENCES supply_items (id),
    CONSTRAINT uq_sa_one_open UNIQUE (shelter_id, item_id, active_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_sa_event_open ON shortage_alerts (event_id, resolved_at);

-- Optional hourly snapshots for post-cyclone reports
CREATE TABLE occupancy_snapshots (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    event_id     BIGINT      NOT NULL,
    shelter_id   BIGINT      NOT NULL,
    occupancy    INT         NOT NULL,
    capacity     INT         NOT NULL,
    taken_at     DATETIME    NOT NULL,
    CONSTRAINT pk_occ_snap PRIMARY KEY (id),
    CONSTRAINT fk_os_event   FOREIGN KEY (event_id)   REFERENCES disaster_events (id),
    CONSTRAINT fk_os_shelter FOREIGN KEY (shelter_id) REFERENCES shelters (id),
    CONSTRAINT uq_os_hour UNIQUE (event_id, shelter_id, taken_at)  -- idempotent per hour
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- JWT logout: revoked token ids are kept until the token would expire anyway
CREATE TABLE revoked_tokens (
    jti         CHAR(36)   NOT NULL,
    expires_at  DATETIME   NOT NULL,
    CONSTRAINT pk_revoked_tokens PRIMARY KEY (jti)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Generic audit trail (no personal data in details)
CREATE TABLE audit_logs (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    actor_id     BIGINT       NULL,
    action       VARCHAR(60)  NOT NULL,
    entity_type  VARCHAR(40)  NOT NULL,
    entity_id    BIGINT       NULL,
    details      VARCHAR(500) NULL,
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_audit_logs PRIMARY KEY (id),
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_audit_entity ON audit_logs (entity_type, entity_id);
