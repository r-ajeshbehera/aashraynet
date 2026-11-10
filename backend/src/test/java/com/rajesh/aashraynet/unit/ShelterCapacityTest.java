package com.rajesh.aashraynet.unit;

import com.rajesh.aashraynet.entity.Shelter;
import com.rajesh.aashraynet.exception.CapacityExceededException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShelterCapacityTest {

    @Test void checkInSucceedsWhenSpaceAvailable() {
        Shelter s = TestData.shelter(1, "A", 19.6, 85.4, 10, 9);
        s.admit(1);
        assertThat(s.getOccupancy()).isEqualTo(10);
        assertThat(s.freePlaces()).isZero();
    }

    @Test void checkInFailsWhenFull() {
        Shelter s = TestData.shelter(1, "A", 19.6, 85.4, 10, 10);
        assertThatThrownBy(() -> s.admit(1)).isInstanceOf(CapacityExceededException.class);
        assertThat(s.getOccupancy()).isEqualTo(10);
    }

    @Test void checkOutReducesOccupancy() {
        Shelter s = TestData.shelter(1, "A", 19.6, 85.4, 10, 4);
        s.release(1);
        assertThat(s.getOccupancy()).isEqualTo(3);
    }

    @Test void occupancyNeverExceedsCapacityForGroups() {
        Shelter s = TestData.shelter(1, "A", 19.6, 85.4, 10, 8);
        assertThatThrownBy(() -> s.admit(3)).isInstanceOf(CapacityExceededException.class);
        assertThat(s.getOccupancy()).isEqualTo(8);
    }

    @Test void occupancyNeverNegative() {
        Shelter s = TestData.shelter(1, "A", 19.6, 85.4, 10, 0);
        assertThatThrownBy(() -> s.release(1)).isInstanceOf(IllegalStateException.class);
    }

    @Test void capacityCannotDropBelowOccupancy() {
        Shelter s = TestData.shelter(1, "A", 19.6, 85.4, 10, 8);
        assertThatThrownBy(() -> s.setCapacity(5)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void percentAndFreePlaces() {
        Shelter s = TestData.shelter(1, "A", 19.6, 85.4, 60, 59);
        assertThat(s.occupancyPercent()).isEqualTo(98.3);
        assertThat(s.freePlaces()).isEqualTo(1);
    }
}
