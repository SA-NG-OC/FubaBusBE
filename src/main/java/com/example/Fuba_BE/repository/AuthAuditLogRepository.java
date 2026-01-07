package com.example.Fuba_BE.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.Fuba_BE.domain.entity.AuthAuditLog;

@Repository
public interface AuthAuditLogRepository extends JpaRepository<AuthAuditLog, Long> {
    
    List<AuthAuditLog> findByUserIdOrderByCreatedAtDesc(Integer userId);
    
    @Query("SELECT al FROM AuthAuditLog al WHERE al.userId = :userId AND al.action = :action AND al.createdAt >= :since")
    List<AuthAuditLog> findRecentActionsByUser(
            @Param("userId") Integer userId, 
            @Param("action") String action, 
            @Param("since") LocalDateTime since
    );
    
    @Query("SELECT al FROM AuthAuditLog al WHERE al.email = :identifier OR al.phoneNumber = :identifier ORDER BY al.createdAt DESC")
    List<AuthAuditLog> findByEmailOrPhoneNumber(
            @Param("identifier") String identifier
    );
}
