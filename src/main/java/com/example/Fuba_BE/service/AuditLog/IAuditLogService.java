package com.example.Fuba_BE.service.AuditLog;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.Fuba_BE.dto.AuditLog.AuditLogResponseDTO;

public interface IAuditLogService {

    /**
     * Log an action performed by a user
     */
    void logAction(Integer userId, String action, String tableName, Integer recordId,
            String oldValue, String newValue, String ipAddress);

    /**
     * Get audit logs for a specific user
     */
    Page<AuditLogResponseDTO> getLogsByUserId(Integer userId, Pageable pageable);

    /**
     * Get audit logs with filters
     */
    Page<AuditLogResponseDTO> getLogsWithFilters(Integer userId, String action, String tableName,
            LocalDateTime startDate, LocalDateTime endDate,
            Pageable pageable);

    /**
     * Get staff activity logs (employees with STAFF role)
     */
    Page<AuditLogResponseDTO> getStaffActivityLogs(String action, LocalDateTime startDate,
            LocalDateTime endDate, Pageable pageable);

    /**
     * Log booking creation (for staff ticket sales tracking)
     */
    void logBookingCreated(Integer staffUserId, Integer bookingId, String customerInfo, String ipAddress);

    /**
     * Log booking cancellation
     */
    void logBookingCancelled(Integer staffUserId, Integer bookingId, String reason, String ipAddress);

    /**
     * Log ticket sale by staff
     */
    void logTicketSale(Integer staffUserId, Integer ticketId, String ticketInfo, String ipAddress);

    /**
     * Log customer status update
     */
    void logCustomerStatusUpdate(Integer adminUserId, Integer customerId, String oldStatus,
            String newStatus, String ipAddress);
}
