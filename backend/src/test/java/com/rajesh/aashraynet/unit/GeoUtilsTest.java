package com.rajesh.aashraynet.unit;

import com.rajesh.aashraynet.service.GeoUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class GeoUtilsTest {

    @Test void samePointIsZeroKm() {
        assertThat(GeoUtils.haversineKm(19.667, 85.433, 19.667, 85.433)).isEqualTo(0.0);
    }

    @Test void oneDegreeOfLatitudeIsAbout111Km() {
        // 2 * pi * 6371 / 360 = 111.19 km
        assertThat(GeoUtils.haversineKm(19.0, 85.0, 20.0, 85.0)).isCloseTo(111.19, within(0.01));
    }

    @Test void distanceIsSymmetric() {
        double ab = GeoUtils.haversineKm(19.667, 85.433, 19.733, 85.680);
        double ba = GeoUtils.haversineKm(19.733, 85.680, 19.667, 85.433);
        assertThat(ab).isCloseTo(ba, within(1e-9));
    }

    @Test void boundingBoxContainsTheRadius() {
        GeoUtils.BoundingBox box = GeoUtils.boundingBox(19.667, 85.433, 50);
        assertThat(GeoUtils.haversineKm(19.667, 85.433, box.maxLat().doubleValue(), 85.433)).isCloseTo(50, within(0.5));
        assertThat(box.minLng().doubleValue()).isLessThan(85.433);
    }
}
