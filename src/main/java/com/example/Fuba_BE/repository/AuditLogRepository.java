package com.example.Fuba_BE.repository;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.Fuba_BE.domain.entity.AuditLog;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {

        /**
         * Find audit logs by user ID with pagination
         */
        @Query("SELECT a FROM AuditLog a WHERE a.user.userId = :userId ORDER BY a.createdAt DESC")
        Page<AuditLog> findByUserId(@Param("userId") Integer userId, Pageable pageable);

        /**
         * Find audit logs by action type
         */
        @Query("SELECT a FROM AuditLog a WHERE a.action = :action ORDER BY a.createdAt DESC")
        Page<AuditLog> findByAction(@Param("action") String action, Pageable pageable);

        /**
         * Find audit logs by table name
         */
        @Query("SELECT a FROM AuditLog a WHERE a.tableName = :tableName ORDER BY a.createdAt DESC")
        Page<AuditLog> findByTableName(@Param("tableName") String tableName, Pageable pageable);

        /**
         * Find audit logs by user ID and action
         */
        @Query("SELECT a FROM AuditLog a WHERE a.user.userId = :userId AND a.action = :action ORDER BY a.createdAt DESC")
        Page<AuditLog> findByUserIdAndAction(@Param("userId") Integer userId, @Param("action") String action,
                        Pageable pageable);

        /**
         * Find audit logs within date range
         */
        @Query("SELECT a FROM AuditLog a WHERE a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
        Page<AuditLog> findByDateRange(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate,
                        Pageable pageable);

        /**
         * Find audit logs with filters (user, action, table, date range)
         */
        @Query("SELECT a FROM AuditLog a LEFT JOIN a.user u WHERE " +
                        "(CAST(:userId AS integer) IS NULL OR a.user.userId = :userId) " +
                        "AND (CAST(:action AS string) IS NULL OR :action = '' OR a.action = :action) " +
                        "AND (CAST(:tableName AS string) IS NULL OR :tableName = '' OR a.tableName = :tableName) " +
                        "AND (CAST(:startDate AS timestamp) IS NULL OR a.createdAt >= :startDate) " +
                        "AND (CAST(:endDate AS timestamp) IS NULL OR a.createdAt <= :endDate) " +
                        "ORDER BY a.createdAt DESC")
        Page<AuditLog> findWithFilters(
                        @Param("userId") Integer userId,
                        @Param("action") String action,
                        @Param("tableName") String tableName,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate,
                        Pageable pageable);

        /**
         * Find staff activity logs (users with STAFF role) with filters
         */
        @Query("SELECT a FROM AuditLog a JOIN a.user u JOIN u.role r WHERE r.roleName = 'STAFF' " +
                        "AND (CAST(:action AS string) IS NULL OR :action = '' OR a.action = :action) " +
                        "AND (CAST(:startDate AS timestamp) IS NULL OR a.createdAt >= :startDate) " +
                        "AND (CAST(:endDate AS timestamp) IS NULL OR a.createdAt <= :endDate) " +
                        "ORDER BY a.createdAt DESC")
        Page<AuditLog> findStaffActivityLogs(
                        @Param("action") String action,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate,
                        Pageable pageable);
}
