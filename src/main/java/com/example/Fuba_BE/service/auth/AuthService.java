package com.example.Fuba_BE.service.auth;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Fuba_BE.domain.entity.RefreshToken;
import com.example.Fuba_BE.domain.entity.Role;
import com.example.Fuba_BE.domain.entity.User;
import com.example.Fuba_BE.dto.Auth.AuthResponse;
import com.example.Fuba_BE.dto.Auth.ForgotPasswordRequest;
import com.example.Fuba_BE.dto.Auth.LoginRequest;
import com.example.Fuba_BE.dto.Auth.RegisterRequest;
import com.example.Fuba_BE.dto.Auth.ResetPasswordRequest;
import com.example.Fuba_BE.exception.BadRequestException;
import com.example.Fuba_BE.exception.NotFoundException;
import com.example.Fuba_BE.exception.UnauthorizedException;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.repository.RoleRepository;
import com.example.Fuba_BE.repository.UserRepository;
import com.example.Fuba_BE.security.JwtTokenProvider;
import com.example.Fuba_BE.service.notification.IEmailService;
import com.example.Fuba_BE.service.notification.ISmsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final IEmailService emailService;
    private final ISmsService smsService;
    private final RefreshTokenService refreshTokenService;
    private final AuthAuditService auditService;

    @Value("${app.security.max-failed-attempts}")
    private int maxFailedAttempts;

    @Value("${app.security.lock-duration-minutes}")
    private int lockDurationMinutes;

    @Override
    @Transactional
    public ApiResponse<AuthResponse> 
    login(LoginRequest request) {
        log.info("Login attempt for: {}", request.getEmailOrPhone());

        // Find user
        User user = userRepository.findByEmailOrPhoneNumber(request.getEmailOrPhone(), request.getEmailOrPhone())
                .orElseThrow(() -> {
                    auditService.logFailedLogin(request.getEmailOrPhone(), "User not found");
                    return new NotFoundException("Invalid email/phone or password");
                });

        // Check if account is locked
        if (user.isAccountLocked()) {
            auditService.logFailedLogin(request.getEmailOrPhone(), "Account is locked");
            throw new LockedException("Account is locked until " + user.getAccountLockedUntil() + 
                                     ". Please try again later or reset your password.");
        }

        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmailOrPhone(),
                            request.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Reset failed attempts on successful login
            user.resetFailedAttempts();
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            // Generate tokens
            String accessToken = tokenProvider.generateToken(authentication);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

            AuthResponse response = AuthResponse.builder()
                    .accessToken(accessToken)
                    .tokenType("Bearer")
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .role(user.getRole().getRoleName())
                    .refreshToken(refreshToken.getToken())
                    .build();

            auditService.logSuccessfulLogin(user.getUserId(), user.getEmail(), user.getPhoneNumber());
            log.info("User logged in successfully: {}", user.getEmail());
            return ApiResponse.success("Login successful", response);

        } catch (BadCredentialsException e) {
            // Increment failed attempts
            user.incrementFailedAttempts();
            
            if (user.getFailedLoginAttempts() >= maxFailedAttempts) {
                user.lockAccount(lockDurationMinutes);
                auditService.logAccountLocked(user.getUserId(), user.getEmail(), user.getPhoneNumber(), 
                                             user.getFailedLoginAttempts());
                userRepository.save(user);
                throw new UnauthorizedException("Account locked due to multiple failed login attempts. " +
                                               "Account will be unlocked after " + lockDurationMinutes + " minutes.");
            }

            userRepository.save(user);
            int attemptsLeft = maxFailedAttempts - user.getFailedLoginAttempts();
            auditService.logFailedLogin(request.getEmailOrPhone(), 
                    "Invalid credentials. Attempts left: " + attemptsLeft);
            
            throw new UnauthorizedException("Invalid email/phone or password. " + attemptsLeft + 
                                           " attempts remaining before account lockout.");
        }
    }

    @Override
    @Transactional
    public ApiResponse<AuthResponse> register(RegisterRequest request) {
        log.info("Registration attempt for email: {}", request.getEmail());

        // Validate password match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        // Check if phone number already exists
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BadRequestException("Phone number already registered");
        }

        // Get default USER role
        Role userRole = roleRepository.findByRoleName("USER")
                .orElseThrow(() -> new NotFoundException("Default USER role not found. Please contact administrator."));

        // Create new user
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(userRole)
                .status("Active")
                .emailVerified(false)
                .failedLoginAttempts(0)
                .build();

        User savedUser = userRepository.save(user);

        // Send welcome email (non-blocking)
        try {
            emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getFullName());
        } catch (Exception e) {
            log.error("Failed to send welcome email", e);
        }

        // Generate tokens for immediate login
        String accessToken = tokenProvider.generateTokenFromUserId(
                savedUser.getUserId(),
                savedUser.getEmail(),
                savedUser.getRole().getRoleName()
        );
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(savedUser);

        AuthResponse response = AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .userId(savedUser.getUserId())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .role(savedUser.getRole().getRoleName())
                .refreshToken(refreshToken.getToken())
                .build();

        auditService.logRegistration(savedUser.getUserId(), savedUser.getEmail(), savedUser.getPhoneNumber());
        log.info("User registered successfully: {}", savedUser.getEmail());
        return ApiResponse.success("Registration successful", response);
    }

    @Override
    @Transactional
    public ApiResponse<String> forgotPassword(ForgotPasswordRequest request) {
        log.info("Password reset request for: {}", request.getEmailOrPhone());

        // Find user by email or phone
        User user = userRepository.findByEmailOrPhoneNumber(request.getEmailOrPhone(), request.getEmailOrPhone())
                .orElseThrow(() -> new NotFoundException("User not found with provided email or phone number"));

        // Generate reset token and save
        String resetToken = UUID.randomUUID().toString();
        user.setResetToken(resetToken);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1)); // Token expires in 1 hour
        userRepository.save(user);

        // Determine if email or phone and send appropriate notification
        boolean isEmail = request.getEmailOrPhone().contains("@");
        
        try {
            if (isEmail) {
                emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
                auditService.logPasswordResetRequest(user.getEmail());
                log.info("Password reset email sent to: {}", user.getEmail());
                return ApiResponse.success("Password reset link sent to your email", 
                        "Check your email for reset instructions. Link expires in 1 hour.");
            } else {
                // Generate 6-digit OTP for SMS
                String otp = String.format("%06d", (int) (Math.random() * 1000000));
                smsService.sendPasswordResetSms(user.getPhoneNumber(), otp);
                auditService.logPasswordResetRequest(user.getPhoneNumber());
                log.info("Password reset OTP sent to: {}", user.getPhoneNumber());
                return ApiResponse.success("Password reset OTP sent to your phone", 
                        "Check your SMS for reset code. Code expires in 10 minutes.");
            }
        } catch (Exception e) {
            log.error("Failed to send password reset notification", e);
            throw new BadRequestException("Failed to send password reset notification. Please try again later.");
        }
    }

    @Override
    @Transactional
    public ApiResponse<String> resetPassword(ResetPasswordRequest request) {
        log.info("Password reset attempt with token");

        // Validate password match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        // Find user by reset token
        User user = userRepository.findAll().stream()
                .filter(u -> request.getToken().equals(u.getResetToken()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        // Check token expiry
        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Reset token has expired. Please request a new one.");
        }

        // Reset password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        user.resetFailedAttempts(); // Also unlock account if locked
        userRepository.save(user);

        auditService.logPasswordReset(user.getUserId(), user.getEmail());
        log.info("Password reset successful for user: {}", user.getEmail());
        
        return ApiResponse.success("Password reset successful", 
                "You can now login with your new password");
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<String> validateResetToken(String token) {
        log.info("Validating reset password token");

        // Find user by reset token
        User user = userRepository.findAll().stream()
                .filter(u -> token.equals(u.getResetToken()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Invalid reset token. The link may have been used or is incorrect."));

        // Check token expiry
        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Reset token has expired. Please request a new password reset link.");
        }

        log.info("Reset token validated successfully for user: {}", user.getEmail());
        return ApiResponse.success("Token is valid", 
                "Please proceed to reset your password using POST request with your new password.");
    }

    @Override
    @Transactional
    public ApiResponse<AuthResponse> refreshToken(String refreshTokenStr) {
        log.info("Refresh token request");

        // Find and verify refresh token
        RefreshToken refreshToken = refreshTokenService.findByToken(refreshTokenStr);
        refreshTokenService.verifyExpiration(refreshToken);

        User user = refreshToken.getUser();

        // Generate new access token
        String accessToken = tokenProvider.generateTokenFromUserId(
                user.getUserId(),
                user.getEmail(),
                user.getRole().getRoleName()
        );

        // Optionally rotate refresh token (create new one and revoke old)
        refreshTokenService.revokeToken(refreshTokenStr);
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);

        AuthResponse response = AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().getRoleName())
                .refreshToken(newRefreshToken.getToken())
                .build();

        auditService.logRefreshToken(user.getUserId(), user.getEmail());
        log.info("Access token refreshed for user: {}", user.getEmail());
        
        return ApiResponse.success("Token refreshed successfully", response);
    }
}
