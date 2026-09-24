package com.shiptrack.dto;

import com.shiptrack.model.Address;
import com.shiptrack.model.Role;
import com.shiptrack.model.User;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public final class UserDtos {

    private UserDtos() {}

    public record UserResponse(
            String id,
            String fullName,
            String email,
            String phone,
            String companyName,
            Role role,
            boolean active,
            String provider,
            String avatarUrl,
            Address defaultAddress,
            Instant lastLoginAt,
            Instant createdAt
    ) {
        public static UserResponse from(User u) {
            return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(),
                    u.getCompanyName(), u.getRole(), u.isActive(), u.getProvider(), u.getAvatarUrl(),
                    u.getDefaultAddress(), u.getLastLoginAt(), u.getCreatedAt());
        }
    }

    public record UpdateProfileRequest(
            String fullName,
            String phone,
            String companyName,
            String avatarUrl,
            Address defaultAddress
    ) {}

    public record UpdateRoleRequest(@NotNull Role role) {}

    public record UpdateStatusRequest(boolean active) {}
}
