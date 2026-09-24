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

@Document(collection = "proof_of_delivery")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProofOfDelivery {
    @Id
    private String id;

    @Indexed
    private String shipmentId;

    private String trackingNumber;
    private String receivedByName;
    private String relationship;

    /** Signature captured on the driver device, stored as a data URL. */
    private String signatureImage;

    @Builder.Default
    private List<String> photoUrls = new ArrayList<>();

    private String notes;
    private GeoPoint deliveryLocation;
    private String capturedBy;

    @Builder.Default
    private boolean verified = false;

    private String verifiedBy;
    private Instant verifiedAt;
    private String rejectionReason;

    @Builder.Default
    private Instant deliveredAt = Instant.now();

    @Builder.Default
    private Instant createdAt = Instant.now();
}
