package com.rajesh.aashraynet.entity;

/** Shelter fill level. Calculated, never stored. */
public enum OccupancyStatus {
    GREEN, YELLOW, RED, FULL;

    /**
     * Below 70% = GREEN, 70%..90% inclusive = YELLOW, above 90% but not full = RED, at capacity = FULL.
     * Integer arithmetic (occupancy*100 vs threshold*capacity) avoids floating-point rounding mistakes.
     */
    public static OccupancyStatus of(int occupancy, int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be > 0");
        }
        if (occupancy >= capacity) return FULL;
        long scaled = (long) occupancy * 100;
        if (scaled < 70L * capacity) return GREEN;
        if (scaled <= 90L * capacity) return YELLOW;
        return RED;
    }
}
