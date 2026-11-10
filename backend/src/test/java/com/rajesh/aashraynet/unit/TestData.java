package com.rajesh.aashraynet.unit;

import com.rajesh.aashraynet.entity.District;
import com.rajesh.aashraynet.entity.Shelter;

import java.math.BigDecimal;

/** Builds entities for pure unit tests (no database). */
public final class TestData {
    private TestData() { }

    public static final District PURI = new District("Puri");

    public static Shelter shelter(long id, String name, double lat, double lng, int capacity, int occupancy) {
        Shelter s = new Shelter(PURI, name, name, BigDecimal.valueOf(lat), BigDecimal.valueOf(lng), capacity);
        s.setIdForTest(id);
        s.setOccupancyForTest(occupancy);
        return s;
    }
}
