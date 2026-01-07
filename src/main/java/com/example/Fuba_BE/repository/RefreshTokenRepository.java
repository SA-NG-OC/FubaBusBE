package com.example.Fuba_BE.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.Fuba_BE.domain.entity.RefreshToken;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    
    Optional<RefreshToken> findByToken(String token);
    
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user.userId = :userId AND rt.revoked = false AND rt.expiryDate > :now")
    Optional<RefreshToken> findValidTokenByUserId(@Param("userId") Integer userId, @Param("now") LocalDateTime now);
    
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :now WHERE rt.user.userId = :userId")
    void revokeAllUserTokens(@Param("userId") Integer userId, @Param("now") LocalDateTime now);
    
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
}
