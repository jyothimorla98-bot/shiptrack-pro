package com.shiptrack.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    @Id
    private String id;

    @Indexed
    private String userId;

    private NotificationType type;
    private String title;
    private String message;
    private String shipmentId;
    private String trackingNumber;

    /** IN_APP, EMAIL, SMS or PUSH. */
    @Builder.Default
    private String channel = "IN_APP";

    @Builder.Default
    private boolean read = false;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
