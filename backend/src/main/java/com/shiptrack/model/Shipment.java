package com.shiptrack.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "shipments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Shipment {
    @Id
    private String id;

    @Indexed(unique = true)
    private String trackingNumber;

    /** Id of the user who created the shipment (customer or business client). */
    @Indexed
    private String ownerId;

    private String ownerEmail;

    private Address sender;
    private Address receiver;
    private PackageDetails packageDetails;

    @Builder.Default
    private ShipmentStatus status = ShipmentStatus.CREATED;

    /** STANDARD, EXPRESS or SAME_DAY. Drives the ETA model. */
    @Builder.Default
    private String serviceType = "STANDARD";

    private GeoPoint currentLocation;
    private String assignedDriverId;
    private String assignedDriverName;
    private String routeId;
    private String podId;

    private Double distanceKm;
    private Instant pickedUpAt;
    private Instant estimatedDeliveryAt;
    private Instant originalEstimatedDeliveryAt;
    private Instant actualDeliveryAt;

    @Builder.Default
    private boolean delayed = false;

    @Builder.Default
    private long delayMinutes = 0;

    private String cancellationReason;
    private String notes;

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private Instant updatedAt = Instant.now();
}
