package com.shiptrack.dto;

import com.shiptrack.model.GeoPoint;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public final class RouteDtos {

    private RouteDtos() {}

    public record PlanRouteRequest(
            List<GeoPoint> waypoints,
            String driverId,
            Double trafficFactor
    ) {}

    public record RouteProgressRequest(
            @NotNull Double latitude,
            @NotNull Double longitude,
            String label
    ) {}
}
