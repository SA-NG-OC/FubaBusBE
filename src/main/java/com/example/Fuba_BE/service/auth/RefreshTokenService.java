package com.example.Fuba_BE.service.auth;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Fuba_BE.domain.entity.RefreshToken;
import com.example.Fuba_BE.domain.entity.User;
import com.example.Fuba_BE.exception.BadRequestException;
import com.example.Fuba_BE.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshTokenDurationMs;

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // Revoke all existing tokens for this user
        refreshTokenRepository.revokeAllUserTokens(user.getUserId(), LocalDateTime.now());

        // Create new refresh token
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(LocalDateTime.now().plusSeconds(refreshTokenDurationMs / 1000))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional(readOnly = true)
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new BadRequestException("Refresh token expired. Please login again.");
        }

        if (token.getRevoked()) {
            throw new BadRequestException("Refresh token has been revoked. Please login again.");
        }

        return token;
    }

    @Transactional(readOnly = true)
    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));
    }

    @Transactional
    public void revokeToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            rt.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(rt);
        });
    }

    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info("Cleaned up expired refresh tokens");
    }
}
