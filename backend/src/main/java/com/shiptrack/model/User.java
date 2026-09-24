package com.shiptrack.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    private String id;

    private String fullName;

    @Indexed(unique = true)
    private String email;

    private String password;
    private String phone;
    private String companyName;

    @Builder.Default
    private Role role = Role.CUSTOMER;

    @Builder.Default
    private boolean active = true;

    /** "local" or an OAuth2 provider name such as "google". */
    @Builder.Default
    private String provider = "local";

    private String avatarUrl;
    private Address defaultAddress;

    private String passwordResetToken;
    private Instant passwordResetExpiry;

    private Instant lastLoginAt;

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private Instant updatedAt = Instant.now();
}
