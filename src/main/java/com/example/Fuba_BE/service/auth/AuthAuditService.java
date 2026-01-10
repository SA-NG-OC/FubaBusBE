package com.example.Fuba_BE.service.auth;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.example.Fuba_BE.domain.entity.AuthAuditLog;
import com.example.Fuba_BE.repository.AuthAuditLogRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthAuditService {

    private final AuthAuditLogRepository auditLogRepository;

    @Transactional
    public void logAuthEvent(Integer userId, String email, String phoneNumber, String action, String status, String details) {
        HttpServletRequest request = getCurrentRequest();
        
        AuthAuditLog auditLog = AuthAuditLog.builder()
                .userId(userId)
                .email(email)
                .phoneNumber(phoneNumber)
                .action(action)
                .status(status)
                .ipAddress(getClientIp(request))
                .userAgent(getUserAgent(request))
                .details(details)
                .createdAt(LocalDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
        log.info("Auth event logged: action={}, status={}, userId={}, email={}", action, status, userId, email);
    }

    @Transactional
    public void logSuccessfulLogin(Integer userId, String email, String phoneNumber) {
        logAuthEvent(userId, email, phoneNumber, "LOGIN", "SUCCESS", "User logged in successfully");
    }

    @Transactional
    public void logFailedLogin(String emailOrPhone, String reason) {
        logAuthEvent(null, emailOrPhone.contains("@") ? emailOrPhone : null, 
                    !emailOrPhone.contains("@") ? emailOrPhone : null, 
                    "LOGIN_FAILED", "FAILURE", reason);
    }

    @Transactional
    public void logRegistration(Integer userId, String email, String phoneNumber) {
        logAuthEvent(userId, email, phoneNumber, "REGISTER", "SUCCESS", "New user registered");
    }

    @Transactional
    public void logPasswordResetRequest(String emailOrPhone) {
        logAuthEvent(null, emailOrPhone.contains("@") ? emailOrPhone : null,
                    !emailOrPhone.contains("@") ? emailOrPhone : null,
                    "PASSWORD_RESET_REQUEST", "SUCCESS", "Password reset requested");
    }

    @Transactional
    public void logPasswordReset(Integer userId, String email) {
        logAuthEvent(userId, email, null, "PASSWORD_RESET", "SUCCESS", "Password reset completed");
    }

    @Transactional
    public void logAccountLocked(Integer userId, String email, String phoneNumber, int attempts) {
        logAuthEvent(userId, email, phoneNumber, "ACCOUNT_LOCKED", "FAILURE", 
                    "Account locked after " + attempts + " failed attempts");
    }

    @Transactional
    public void logRefreshToken(Integer userId, String email) {
        logAuthEvent(userId, email, null, "REFRESH_TOKEN", "SUCCESS", "Access token refreshed");
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            log.warn("Could not get current HTTP request", e);
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "UNKNOWN";
        
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private String getUserAgent(HttpServletRequest request) {
        if (request == null) return "UNKNOWN";
        return request.getHeader("User-Agent");
    }
}
