package com.shiptrack.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "tracking_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingEvent {
    @Id
    private String id;

    @Indexed
    private String shipmentId;

    private String trackingNumber;
    private ShipmentStatus status;
    private String description;
    private GeoPoint location;
    private String recordedBy;

    @Builder.Default
    private Instant occurredAt = Instant.now();
}
