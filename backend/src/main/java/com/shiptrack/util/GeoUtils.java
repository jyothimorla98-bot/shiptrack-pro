package com.shiptrack.util;

import com.shiptrack.model.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public final class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0088;

    private GeoUtils() {}

    /** Great-circle distance in kilometres. */
    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public static double distanceKm(GeoPoint a, GeoPoint b) {
        if (a == null || b == null) return 0d;
        return distanceKm(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
    }

    /** Total length of a path through the given points. */
    public static double pathLengthKm(List<GeoPoint> points) {
        if (points == null || points.size() < 2) return 0d;
        double total = 0d;
        for (int i = 1; i < points.size(); i++) {
            total += distanceKm(points.get(i - 1), points.get(i));
        }
        return total;
    }

    /** Evenly spaced points between two coordinates, used to sketch a road-free route line. */
    public static List<GeoPoint> interpolate(GeoPoint from, GeoPoint to, int segments) {
        List<GeoPoint> points = new ArrayList<>();
        if (from == null || to == null || segments < 1) return points;
        for (int i = 1; i < segments; i++) {
            double fraction = (double) i / segments;
            double lat = from.getLatitude() + (to.getLatitude() - from.getLatitude()) * fraction;
            double lng = from.getLongitude() + (to.getLongitude() - from.getLongitude()) * fraction;
            points.add(GeoPoint.of(round(lat), round(lng), "Waypoint " + i));
        }
        return points;
    }

    /** Nearest-neighbour ordering of stops, starting from the first point. */
    public static List<GeoPoint> nearestNeighbourOrder(List<GeoPoint> points) {
        List<GeoPoint> remaining = new ArrayList<>(points);
        List<GeoPoint> ordered = new ArrayList<>();
        if (remaining.isEmpty()) return ordered;

        GeoPoint current = remaining.remove(0);
        ordered.add(current);
        while (!remaining.isEmpty()) {
            GeoPoint best = remaining.get(0);
            double bestDistance = distanceKm(current, best);
            for (GeoPoint candidate : remaining) {
                double d = distanceKm(current, candidate);
                if (d < bestDistance) {
                    bestDistance = d;
                    best = candidate;
                }
            }
            remaining.remove(best);
            ordered.add(best);
            current = best;
        }
        return ordered;
    }

    public static double round(double value) {
        return Math.round(value * 10000d) / 10000d;
    }

    public static double roundKm(double value) {
        return Math.round(value * 100d) / 100d;
    }
}
