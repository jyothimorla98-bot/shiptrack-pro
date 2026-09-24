package com.shiptrack.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "routes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutePlan {
    @Id
    private String id;

    @Indexed
    private String shipmentId;

    private String trackingNumber;
    private String driverId;
    private String driverName;

    @Builder.Default
    private List<GeoPoint> waypoints = new ArrayList<>();

    /** Positions actually visited, appended as live updates come in. */
    @Builder.Default
    private List<GeoPoint> travelledPath = new ArrayList<>();

    private Double distanceKm;
    private Integer estimatedDurationMinutes;

    /** 1.0 = free flowing, 1.6 = heavy congestion. */
    @Builder.Default
    private Double trafficFactor = 1.0;

    @Builder.Default
    private boolean optimized = false;

    @Builder.Default
    private String status = "PLANNED";

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private Instant updatedAt = Instant.now();
}
