package com.example.Fuba_BE.controller;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.Fuba_BE.dto.AuditLog.AuditLogResponseDTO;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.service.AuditLog.IAuditLogService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for audit log management
 * Used to track and retrieve activity logs for staff and admin
 */
@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
@Slf4j
public class AuditLogController {

    private final IAuditLogService auditLogService;

    /**
     * Get all audit logs with filters
     * ADMIN can view all audit logs
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<AuditLogResponseDTO>>> getAuditLogs(
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String tableName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(page = 0, size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("📥 Request to get audit logs - userId: {}, action: {}, table: {}", userId, action, tableName);

        Page<AuditLogResponseDTO> logs = auditLogService.getLogsWithFilters(
                userId, action, tableName, startDate, endDate, pageable);

        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved successfully", logs));
    }

    /**
     * Get audit logs for a specific user
     * ADMIN can view user's audit logs
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<AuditLogResponseDTO>>> getUserAuditLogs(
            @PathVariable Integer userId,
            @PageableDefault(page = 0, size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("📥 Request to get audit logs for user ID: {}", userId);

        Page<AuditLogResponseDTO> logs = auditLogService.getLogsByUserId(userId, pageable);

        return ResponseEntity.ok(ApiResponse.success("User audit logs retrieved successfully", logs));
    }

    /**
     * Get staff activity logs (employees with STAFF role)
     * ADMIN can view staff activities
     */
    @GetMapping("/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<AuditLogResponseDTO>>> getStaffActivityLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(page = 0, size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("📥 Request to get staff activity logs - action: {}, from: {} to: {}", action, startDate, endDate);

        Page<AuditLogResponseDTO> logs = auditLogService.getStaffActivityLogs(action, startDate, endDate, pageable);

        return ResponseEntity.ok(ApiResponse.success("Staff activity logs retrieved successfully", logs));
    }
}
