package com.shiptrack.controller;

import com.shiptrack.dto.AuthDtos;
import com.shiptrack.dto.CommonDtos;
import com.shiptrack.security.UserPrincipal;
import com.shiptrack.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthDtos.AuthResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public AuthDtos.AuthResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/oauth2")
    public AuthDtos.AuthResponse oauthLogin(@Valid @RequestBody AuthDtos.OAuthLoginRequest request) {
        return authService.oauthLogin(request);
    }

    @PostMapping("/forgot-password")
    public CommonDtos.ApiMessage forgotPassword(@Valid @RequestBody AuthDtos.ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public CommonDtos.ApiMessage resetPassword(@Valid @RequestBody AuthDtos.ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }

    @PostMapping("/change-password")
    public CommonDtos.ApiMessage changePassword(@AuthenticationPrincipal UserPrincipal principal,
                                                @Valid @RequestBody AuthDtos.ChangePasswordRequest request) {
        return authService.changePassword(principal.getUser(), request);
    }
}
