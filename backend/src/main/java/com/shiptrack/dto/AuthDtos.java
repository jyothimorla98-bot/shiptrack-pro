package com.shiptrack.dto;

import com.shiptrack.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request and response payloads for authentication and account recovery. */
public final class AuthDtos {

    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank String fullName,
            @Email @NotBlank String email,
            @NotBlank @Size(min = 6, message = "Password must be at least 6 characters") String password,
            String phone,
            String companyName,
            Role role
    ) {}

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {}

    /** Demo OAuth2 exchange: the browser completes the provider flow and posts the profile here. */
    public record OAuthLoginRequest(
            @NotBlank String provider,
            @Email @NotBlank String email,
            String fullName,
            String avatarUrl
    ) {}

    public record AuthResponse(
            String token,
            String tokenType,
            long expiresInMs,
            UserDtos.UserResponse user
    ) {}

    public record ForgotPasswordRequest(@Email @NotBlank String email) {}

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 6) String newPassword
    ) {}

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 6) String newPassword
    ) {}
}
