package com.rajesh.aashraynet.unit;

import com.rajesh.aashraynet.entity.OccupancyStatus;
import org.junit.jupiter.api.Test;

import static com.rajesh.aashraynet.entity.OccupancyStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OccupancyStatusTest {
    @Test void sixtyNinePercentIsGreen()      { assertThat(OccupancyStatus.of(69, 100)).isEqualTo(GREEN); }
    @Test void seventyPercentIsYellow()       { assertThat(OccupancyStatus.of(70, 100)).isEqualTo(YELLOW); }
    @Test void ninetyPercentIsYellow()        { assertThat(OccupancyStatus.of(90, 100)).isEqualTo(YELLOW); }
    @Test void ninetyOnePercentIsRed()        { assertThat(OccupancyStatus.of(91, 100)).isEqualTo(RED); }
    @Test void hundredPercentIsFull()         { assertThat(OccupancyStatus.of(100, 100)).isEqualTo(FULL); }
    @Test void oneBelowCapacityIsRedNotFull() { assertThat(OccupancyStatus.of(59, 60)).isEqualTo(RED); }

    @Test void worksWithoutFloatingPointErrors() {
        // 7/10 = exactly 70% -> YELLOW (a double comparison could give 69.999...)
        assertThat(OccupancyStatus.of(7, 10)).isEqualTo(YELLOW);
    }

    @Test void capacityMustBePositive() {
        assertThatThrownBy(() -> OccupancyStatus.of(0, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
