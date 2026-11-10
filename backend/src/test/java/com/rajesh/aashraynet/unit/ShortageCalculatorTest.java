package com.rajesh.aashraynet.unit;

import com.rajesh.aashraynet.service.ShortageCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ShortageCalculatorTest {
    private static BigDecimal bd(String v) { return new BigDecimal(v); }

    @Test void daysLeftCalculation() {
        // 150 kg rice / (499 people x 0.40 kg) = 0.7515 -> 0.75 days
        assertThat(ShortageCalculator.daysRemaining(bd("150"), 499, bd("0.40"))).contains(bd("0.75"));
    }

    @Test void zeroOccupancyCannotBeCalculated() {
        assertThat(ShortageCalculator.daysRemaining(bd("100"), 0, bd("0.40"))).isEmpty();
    }

    @Test void zeroConsumptionRateCannotBeCalculated() {
        assertThat(ShortageCalculator.daysRemaining(bd("100"), 50, BigDecimal.ZERO)).isEmpty();
    }

    @Test void belowOneDayIsShortage() {
        assertThat(ShortageCalculator.isShortage(bd("0.99"))).isTrue();
    }

    @Test void exactlyOneDayIsNotShortage() {
        BigDecimal days = ShortageCalculator.daysRemaining(bd("200"), 50, bd("4.00")).orElseThrow();
        assertThat(days).isEqualByComparingTo("1");
        assertThat(ShortageCalculator.isShortage(days)).isFalse();
    }

    @Test void emptyStockIsZeroDays() {
        assertThat(ShortageCalculator.daysRemaining(BigDecimal.ZERO, 10, bd("4.00"))).contains(bd("0.00"));
        assertThat(ShortageCalculator.daysRemaining(null, 10, bd("4.00"))).contains(bd("0.00"));
    }

    @Test void roundsDownSoAlmostOneDayIsStillShortage() {
        // 199.9 / 200 = 0.9995 -> 0.99 (not rounded up to 1.00)
        BigDecimal days = ShortageCalculator.daysRemaining(bd("199.9"), 50, bd("4.00")).orElseThrow();
        assertThat(ShortageCalculator.isShortage(days)).isTrue();
    }
}
