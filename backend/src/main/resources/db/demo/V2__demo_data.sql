-- =====================================================================
-- DEMONSTRATION DATA ONLY (loaded by the 'dev' profile).
-- Fictional event, people and stock. Shelter coordinates are approximate.
-- Every demo account password: Demo@1234   (development only!)
-- =====================================================================

INSERT INTO districts (id, name) VALUES (1, 'Puri'), (2, 'Kendrapara');

SET @pw = '$2a$12$WqQ9oB4wH0miXWH9wk2YQeXXaimEwktYzievjQlfmqYDePztcyDEW';
INSERT INTO users (id, full_name, email, password_hash, role, district_id) VALUES
 (1, 'State Admin (Demo)',            'state@aashraynet.demo',               @pw, 'STATE_ADMIN',      NULL),
 (2, 'Puri District Admin (Demo)',    'puri.admin@aashraynet.demo',          @pw, 'DISTRICT_ADMIN',   1),
 (3, 'Kendrapara District Admin (Demo)','kendrapara.admin@aashraynet.demo',  @pw, 'DISTRICT_ADMIN',   2),
 (4, 'Satapada In-charge (Demo)',     'satapada.incharge@aashraynet.demo',   @pw, 'SHELTER_INCHARGE', 1),
 (5, 'Brahmagiri In-charge (Demo)',   'brahmagiri.incharge@aashraynet.demo', @pw, 'SHELTER_INCHARGE', 1),
 (6, 'Mahakalapada In-charge (Demo)', 'mahakalapada.incharge@aashraynet.demo',@pw,'SHELTER_INCHARGE', 2),
 (7, 'Puri Volunteer (Demo)',         'puri.volunteer@aashraynet.demo',      @pw, 'VOLUNTEER',        1),
 (8, 'Kendrapara Volunteer (Demo)',   'kendrapara.volunteer@aashraynet.demo',@pw, 'VOLUNTEER',        2);

INSERT INTO disaster_events (id, name, start_date, end_date, active, created_by)
VALUES (1, 'Cyclone Dana 2026 (DEMO)', '2026-10-23', NULL, TRUE, 1);

INSERT INTO shelters (id, district_id, name, village, latitude, longitude, capacity, has_medical, has_ramp, has_generator, incharge_id) VALUES
 (1, 1, 'Satapada MCS',      'Satapada',      19.667000, 85.433000, 60, TRUE,  TRUE,  TRUE,  4),
 (2, 1, 'Brahmagiri MCS',    'Brahmagiri',    19.733000, 85.680000, 80, FALSE, TRUE,  TRUE,  5),
 (3, 1, 'Krushnaprasad MCS', 'Krushnaprasad', 19.702000, 85.378000, 50, TRUE,  FALSE, FALSE, NULL),
 (4, 1, 'Puri Town MCS',     'Puri',          19.810000, 85.830000, 40, TRUE,  TRUE,  TRUE,  NULL),
 (5, 2, 'Mahakalapada MCS',  'Mahakalapada',  20.390000, 86.670000, 70, FALSE, TRUE,  TRUE,  6),
 (6, 2, 'Rajnagar MCS',      'Rajnagar',      20.470000, 86.800000, 60, TRUE,  FALSE, TRUE,  NULL);

-- Evacuees: n people per shelter, fictional names, some special needs.
CREATE TEMPORARY TABLE demo_fill (shelter_id BIGINT, people INT, village VARCHAR(60), registered_by BIGINT);
INSERT INTO demo_fill VALUES (1,59,'Satapada',4),(2,30,'Brahmagiri',5),(3,10,'Krushnaprasad',7),(4,30,'Puri',7),(5,70,'Mahakalapada',6),(6,20,'Rajnagar',8);

INSERT INTO evacuees (event_id, shelter_id, full_name, name_normalized, age, gender, village, village_normalized,
                      special_need, checked_in_at, registered_by)
WITH RECURSIVE seq (n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 70)
SELECT 1, f.shelter_id,
       CONCAT('Demo Evacuee ', f.shelter_id, '-', s.n),
       LOWER(CONCAT('demo evacuee ', f.shelter_id, '-', s.n)),
       CASE WHEN MOD(s.n, 7) = 0 THEN 68 + MOD(s.n, 10) WHEN MOD(s.n, 11) = 0 THEN 1 ELSE 18 + MOD(s.n * 7, 40) END,
       CASE WHEN MOD(s.n, 2) = 0 THEN 'FEMALE' ELSE 'MALE' END,
       f.village, LOWER(f.village),
       CASE WHEN MOD(s.n, 7) = 0 THEN 'ELDERLY' WHEN MOD(s.n, 11) = 0 THEN 'INFANT'
            WHEN MOD(s.n, 13) = 0 THEN 'PREGNANT' WHEN MOD(s.n, 17) = 0 THEN 'DISABLED' ELSE 'NONE' END,
       TIMESTAMP('2026-10-23 18:00:00') + INTERVAL s.n MINUTE,
       f.registered_by
FROM demo_fill f JOIN seq s ON s.n <= f.people;

-- One named person for trying the public family finder: name "Laxmi Nayak", village "Satapada".
UPDATE evacuees SET full_name = 'Laxmi Nayak', name_normalized = 'laxmi nayak', gender = 'FEMALE', age = 34, special_need = 'NONE'
WHERE shelter_id = 1 AND full_name = 'Demo Evacuee 1-1';

DROP TEMPORARY TABLE demo_fill;

-- Occupancy counter = active evacuees (kept in sync by the application from now on)
UPDATE shelters s SET s.occupancy = (SELECT COUNT(*) FROM evacuees e WHERE e.shelter_id = s.id AND e.checked_out_at IS NULL);

INSERT INTO supply_items (id, name, unit, per_person_per_day) VALUES
 (1, 'Rice', 'kg', 0.400), (2, 'Drinking water', 'litre', 4.000), (3, 'Dal', 'kg', 0.100),
 (4, 'Blanket', 'piece', 0), (5, 'Medicine kit', 'kit', 0), (6, 'Hygiene kit', 'kit', 0);

INSERT INTO shelter_stock (shelter_id, item_id, quantity) VALUES
 (1,1,20),(1,2,300),(1,3,10),(1,4,40),
 (2,1,100),(2,2,50),(2,3,15),
 (3,1,60),(3,2,200),(3,3,8),
 (4,1,30),(4,2,500),(4,3,6),
 (5,1,25),(5,2,250),(5,3,5),(5,4,30),
 (6,1,80),(6,2,400),(6,3,9);

INSERT INTO stock_movements (shelter_id, item_id, event_id, movement_type, quantity_delta, balance_after, reason, performed_by)
SELECT shelter_id, item_id, 1, 'INITIAL', quantity, quantity, 'Demo opening stock', 1 FROM shelter_stock;

INSERT INTO supply_requests (id, event_id, shelter_id, status, requested_by, note)
VALUES (1, 1, 1, 'REQUESTED', 4, 'Rice running low (demo)');
INSERT INTO request_lines (request_id, item_id, quantity) VALUES (1, 1, 100), (1, 2, 500);
INSERT INTO supply_request_history (request_id, from_status, to_status, changed_by) VALUES (1, NULL, 'REQUESTED', 4);
