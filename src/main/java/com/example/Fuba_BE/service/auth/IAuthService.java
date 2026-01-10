package com.example.Fuba_BE.service.auth;

import com.example.Fuba_BE.dto.Auth.AuthResponse;
import com.example.Fuba_BE.dto.Auth.ForgotPasswordRequest;
import com.example.Fuba_BE.dto.Auth.LoginRequest;
import com.example.Fuba_BE.dto.Auth.RegisterRequest;
import com.example.Fuba_BE.dto.Auth.ResetPasswordRequest;
import com.example.Fuba_BE.payload.ApiResponse;

public interface IAuthService {
    
    /**
     * Authenticate user with email/phone and password
     * @param request login credentials
     * @return authentication response with JWT token
     */
    ApiResponse<AuthResponse> login(LoginRequest request);
    
    /**
     * Register new user account
     * @param request registration details
     * @return authentication response with JWT token
     */
    ApiResponse<AuthResponse> register(RegisterRequest request);
    
    /**
     * Initiate password reset process
     * @param request forgot password request with email or phone
     * @return success message
     */
    ApiResponse<String> forgotPassword(ForgotPasswordRequest request);
    
    /**
     * Reset password with token
     * @param request reset password request with token and new password
     * @return success message
     */
    ApiResponse<String> resetPassword(ResetPasswordRequest request);
    
    /**
     * Refresh access token using refresh token
     * @param refreshToken the refresh token
     * @return new access token and refresh token
     */
    ApiResponse<AuthResponse> refreshToken(String refreshToken);
}
