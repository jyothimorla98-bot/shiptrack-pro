package com.shiptrack.service;

import com.shiptrack.dto.AuthDtos;
import com.shiptrack.dto.CommonDtos;
import com.shiptrack.dto.UserDtos;
import com.shiptrack.exception.ApiException;
import com.shiptrack.model.ActivityLog;
import com.shiptrack.model.Role;
import com.shiptrack.model.User;
import com.shiptrack.repository.ActivityLogRepository;
import com.shiptrack.repository.UserRepository;
import com.shiptrack.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final ActivityLogRepository activityLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final NotificationService notificationService;

    public AuthService(UserRepository userRepository,
                       ActivityLogRepository activityLogRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       NotificationService notificationService) {
        this.userRepository = userRepository;
        this.activityLogRepository = activityLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.notificationService = notificationService;
    }

    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw ApiException.conflict("An account already uses this email address");
        }

        // Staff roles are granted by an administrator, never self-selected at signup.
        Role requested = request.role() == null ? Role.CUSTOMER : request.role();
        Role role = (requested == Role.CUSTOMER || requested == Role.BUSINESS_CLIENT) ? requested : Role.CUSTOMER;

        User user = User.builder()
                .fullName(request.fullName())
                .email(request.email().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .phone(request.phone())
                .companyName(request.companyName())
                .role(role)
                .build();

        User saved = userRepository.save(user);
        logActivity(saved, "REGISTER", "Account created");
        return buildResponse(saved);
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (BadCredentialsException ex) {
            throw new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "Email or password is incorrect");
        }

        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> ApiException.notFound("Account not found"));
        if (!user.isActive()) {
            throw ApiException.forbidden("This account has been deactivated. Contact an administrator.");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        logActivity(user, "LOGIN", "Signed in with email and password");
        return buildResponse(user);
    }

    /**
     * Demo OAuth2 exchange. The browser completes the provider flow and posts the verified
     * profile here; in production, verify the provider id token server side before trusting it.
     */
    public AuthDtos.AuthResponse oauthLogin(AuthDtos.OAuthLoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseGet(() -> userRepository.save(User.builder()
                        .fullName(request.fullName() == null ? request.email() : request.fullName())
                        .email(request.email().toLowerCase())
                        .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                        .provider(request.provider())
                        .avatarUrl(request.avatarUrl())
                        .role(Role.CUSTOMER)
                        .build()));

        if (!user.isActive()) {
            throw ApiException.forbidden("This account has been deactivated. Contact an administrator.");
        }
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        logActivity(user, "LOGIN", "Signed in with " + request.provider());
        return buildResponse(user);
    }

    public CommonDtos.ApiMessage forgotPassword(AuthDtos.ForgotPasswordRequest request) {
        userRepository.findByEmailIgnoreCase(request.email()).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            user.setPasswordResetToken(token);
            user.setPasswordResetExpiry(Instant.now().plus(Duration.ofMinutes(30)));
            userRepository.save(user);
            log.info("Password reset token for {}: {}", user.getEmail(), token);
            notificationService.deliverEmail(user.getEmail(), "Reset your password",
                    "Use this code to choose a new password within 30 minutes: " + token);
        });
        // The same reply either way, so the endpoint cannot be used to discover registered emails.
        return new CommonDtos.ApiMessage("If that email is registered, a reset link is on its way");
    }

    public CommonDtos.ApiMessage resetPassword(AuthDtos.ResetPasswordRequest request) {
        User user = userRepository.findByPasswordResetToken(request.token())
                .orElseThrow(() -> ApiException.badRequest("This reset link is not valid"));

        if (user.getPasswordResetExpiry() == null || user.getPasswordResetExpiry().isBefore(Instant.now())) {
            throw ApiException.badRequest("This reset link has expired. Request a new one.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiry(null);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        logActivity(user, "PASSWORD_RESET", "Password changed with a reset link");
        return new CommonDtos.ApiMessage("Password updated. You can sign in now.");
    }

    public CommonDtos.ApiMessage changePassword(User user, AuthDtos.ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw ApiException.badRequest("Your current password does not match");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        logActivity(user, "PASSWORD_CHANGE", "Password changed from account settings");
        return new CommonDtos.ApiMessage("Password updated");
    }

    private AuthDtos.AuthResponse buildResponse(User user) {
        return new AuthDtos.AuthResponse(
                jwtService.generateToken(user),
                "Bearer",
                jwtService.getExpirationMs(),
                UserDtos.UserResponse.from(user));
    }

    private void logActivity(User user, String action, String detail) {
        activityLogRepository.save(ActivityLog.builder()
                .userId(user.getId())
                .userEmail(user.getEmail())
                .action(action)
                .detail(detail)
                .build());
    }
}
