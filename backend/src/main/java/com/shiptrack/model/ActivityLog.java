package com.shiptrack.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "activity_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLog {
    @Id
    private String id;

    @Indexed
    private String userId;

    private String userEmail;
    private String action;
    private String detail;
    private String ipAddress;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
