package com.shiptrack.util;

import com.shiptrack.model.GeoPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeoUtilsTest {

    private static final GeoPoint HYDERABAD = GeoPoint.of(17.3850, 78.4867, "Hyderabad");
    private static final GeoPoint BENGALURU = GeoPoint.of(12.9716, 77.5946, "Bengaluru");
    private static final GeoPoint CHENNAI = GeoPoint.of(13.0827, 80.2707, "Chennai");

    @Test
    @DisplayName("Hyderabad to Bengaluru is roughly 500 km as the crow flies")
    void distanceBetweenCities() {
        double km = GeoUtils.distanceKm(HYDERABAD, BENGALURU);
        assertTrue(km > 450 && km < 520, "Expected ~500 km but got " + km);
    }

    @Test
    void distanceToSelfIsZero() {
        assertEquals(0d, GeoUtils.distanceKm(HYDERABAD, HYDERABAD), 0.0001);
    }

    @Test
    void nullPointsGiveZeroDistance() {
        assertEquals(0d, GeoUtils.distanceKm(null, BENGALURU));
        assertEquals(0d, GeoUtils.distanceKm(HYDERABAD, null));
    }

    @Test
    void pathLengthAddsUpEachLeg() {
        double legs = GeoUtils.distanceKm(HYDERABAD, BENGALURU) + GeoUtils.distanceKm(BENGALURU, CHENNAI);
        double path = GeoUtils.pathLengthKm(List.of(HYDERABAD, BENGALURU, CHENNAI));
        assertEquals(legs, path, 0.001);
    }

    @Test
    void pathLengthOfASinglePointIsZero() {
        assertEquals(0d, GeoUtils.pathLengthKm(List.of(HYDERABAD)));
        assertEquals(0d, GeoUtils.pathLengthKm(null));
    }

    @Test
    @DisplayName("Interpolation returns segments-1 waypoints that stay on the line")
    void interpolateProducesWaypoints() {
        List<GeoPoint> points = GeoUtils.interpolate(HYDERABAD, BENGALURU, 5);
        assertEquals(4, points.size());
        for (GeoPoint point : points) {
            assertTrue(point.getLatitude() < HYDERABAD.getLatitude());
            assertTrue(point.getLatitude() > BENGALURU.getLatitude());
        }
    }

    @Test
    void nearestNeighbourKeepsTheStartAndVisitsEveryStop() {
        List<GeoPoint> ordered = GeoUtils.nearestNeighbourOrder(List.of(HYDERABAD, CHENNAI, BENGALURU));
        assertEquals(3, ordered.size());
        assertEquals("Hyderabad", ordered.get(0).getLabel());
        assertEquals("Bengaluru", ordered.get(1).getLabel());
        assertEquals("Chennai", ordered.get(2).getLabel());
    }

    @Test
    void trackingNumbersLookRightAndDoNotRepeat() {
        String first = TrackingNumberGenerator.next();
        assertTrue(first.matches("STP-\\d{6}-[A-Z2-9]{6}"), "Unexpected format: " + first);
        assertNotEquals(first, TrackingNumberGenerator.next());
    }
}
