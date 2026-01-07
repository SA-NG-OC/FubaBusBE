package com.example.Fuba_BE.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Fuba_BE.dto.Auth.AuthResponse;
import com.example.Fuba_BE.dto.Auth.ForgotPasswordRequest;
import com.example.Fuba_BE.dto.Auth.LoginRequest;
import com.example.Fuba_BE.dto.Auth.RegisterRequest;
import com.example.Fuba_BE.dto.Auth.ResetPasswordRequest;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.service.auth.IAuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received for: {}", request.getEmailOrPhone());
        ApiResponse<AuthResponse> response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request received for: {}", request.getEmail());
        ApiResponse<AuthResponse> response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Forgot password request received for: {}", request.getEmailOrPhone());
        ApiResponse<String> response = authService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Reset password request received");
        ApiResponse<String> response = authService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@RequestBody String refreshToken) {
        log.info("Refresh token request received");
        ApiResponse<AuthResponse> response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }
}
