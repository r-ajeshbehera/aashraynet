package com.rajesh.aashraynet.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/** days remaining = stock / (occupancy × per-person daily need). Pure function: easy to unit test. */
public final class ShortageCalculator {
    private ShortageCalculator() { }

    /** Empty when it cannot be calculated (nobody in the shelter, or the item is not consumption-tracked). */
    public static Optional<BigDecimal> daysRemaining(BigDecimal quantity, int occupancy, BigDecimal perPersonPerDay) {
        if (occupancy <= 0 || perPersonPerDay == null || perPersonPerDay.signum() <= 0) {
            return Optional.empty();
        }
        BigDecimal qty = quantity == null ? BigDecimal.ZERO : quantity;
        BigDecimal dailyNeed = perPersonPerDay.multiply(BigDecimal.valueOf(occupancy));
        // Round DOWN: 0.999 days must still count as a shortage.
        return Optional.of(qty.divide(dailyNeed, 2, RoundingMode.DOWN));
    }

    public static boolean isShortage(BigDecimal daysRemaining) {
        return daysRemaining != null && daysRemaining.compareTo(BigDecimal.ONE) < 0;
    }
}
