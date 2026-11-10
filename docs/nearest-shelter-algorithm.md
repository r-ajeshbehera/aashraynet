# Nearest Available Shelter

**Endpoint:** `GET /api/shelters/{id}/nearest-available?count=3&people=1&radiusKm=50`

**Code:**
- `NearestShelterService.nearestFrom()`: database step
- `NearestShelterService.rank()`: pure ranking step, unit-tested

## Steps

1. **Bounding box.** `GeoUtils.boundingBox(lat, lng, radiusKm)` builds a rectangle around the circle:
   - Δlat = r / 111.32
   - Δlng = r / (111.32 · cos lat)
2. **Database filter** (`ShelterRepository.findCandidates`):
   - `active = true`
   - `id <> current`
   - `capacity - occupancy >= people`
   - latitude and longitude `BETWEEN` the box edges
3. **Exact distance:** Haversine, with R = 6371 km.

   ```
   a = sin²(Δφ/2) + cos φ1 · cos φ2 · sin²(Δλ/2)
   d = 2R · atan2(√a, √(1−a))
   ```

4. Keep shelters with `d <= radiusKm`, sort by distance (then by name), and take `count`.
5. Return each shelter's name, district, village, distance in km, capacity, occupancy, free places, status and facilities.

`rank()` checks the active, free-places and exclusion rules again in Java (defence in depth). That lets those rules be unit-tested without a database.

## Rules

- A family of 20 is never sent to a shelter with 10 free places (`familyOfTwentySkipsShelterWithOnlyTenFree`).
- Results may include a neighbouring district (decision D3). Shelter names and capacities are public anyway; evacuee data never crosses districts.
- This is a **recommendation only**. Nobody is transferred automatically.
- Input limits: people 1–10 000, count 1–20, radius 0–500 km. Anything outside returns 400.

## Scaling

The bounding box lets MySQL use the latitude/longitude columns, so only a handful of rows reach Java. For thousands of shelters, use a `POINT` column with a SPATIAL index and `ST_Distance_Sphere` in SQL.

## Verified

The 8 ranking unit tests and 4 Haversine tests were run (see testing.md).
