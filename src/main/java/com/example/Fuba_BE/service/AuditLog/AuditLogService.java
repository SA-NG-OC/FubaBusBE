package com.example.Fuba_BE.service.AuditLog;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Fuba_BE.domain.entity.AuditLog;
import com.example.Fuba_BE.domain.entity.User;
import com.example.Fuba_BE.dto.AuditLog.AuditLogResponseDTO;
import com.example.Fuba_BE.exception.ResourceNotFoundException;
import com.example.Fuba_BE.mapper.AuditLogMapper;
import com.example.Fuba_BE.repository.AuditLogRepository;
import com.example.Fuba_BE.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuditLogService implements IAuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final AuditLogMapper auditLogMapper;

    @Override
    public void logAction(Integer userId, String action, String tableName, Integer recordId,
            String oldValue, String newValue, String ipAddress) {
        log.info("📝 Logging action: {} by user {} on table {} record {}",
                action, userId, tableName, recordId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        AuditLog auditLog = new AuditLog();
        auditLog.setUser(user);
        auditLog.setAction(action);
        auditLog.setTableName(tableName);
        auditLog.setRecordId(recordId);
        auditLog.setOldValue(oldValue);
        auditLog.setNewValue(newValue);
        auditLog.setIpAddress(ipAddress);

        auditLogRepository.save(auditLog);
        log.info("✅ Action logged successfully: {} by user {}", action, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponseDTO> getLogsByUserId(Integer userId, Pageable pageable) {
        log.info("📥 Getting audit logs for user ID: {}", userId);
        Page<AuditLog> logs = auditLogRepository.findByUserId(userId, pageable);
        return logs.map(auditLogMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponseDTO> getLogsWithFilters(Integer userId, String action, String tableName,
            LocalDateTime startDate, LocalDateTime endDate,
            Pageable pageable) {
        log.info("📥 Getting audit logs with filters - userId: {}, action: {}, table: {}",
                userId, action, tableName);
        Page<AuditLog> logs = auditLogRepository.findWithFilters(userId, action, tableName,
                startDate, endDate, pageable);
        return logs.map(auditLogMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponseDTO> getStaffActivityLogs(String action, LocalDateTime startDate,
            LocalDateTime endDate, Pageable pageable) {
        log.info("📥 Getting staff activity logs - action: {}, from: {} to: {}",
                action, startDate, endDate);
        Page<AuditLog> logs = auditLogRepository.findStaffActivityLogs(action, startDate, endDate, pageable);
        return logs.map(auditLogMapper::toResponseDTO);
    }

    @Override
    public void logBookingCreated(Integer staffUserId, Integer bookingId, String customerInfo, String ipAddress) {
        logAction(staffUserId, "BOOKING_CREATED", "bookings", bookingId,
                null, customerInfo, ipAddress);
    }

    @Override
    public void logBookingCancelled(Integer staffUserId, Integer bookingId, String reason, String ipAddress) {
        logAction(staffUserId, "BOOKING_CANCELLED", "bookings", bookingId,
                null, reason, ipAddress);
    }

    @Override
    public void logTicketSale(Integer staffUserId, Integer ticketId, String ticketInfo, String ipAddress) {
        logAction(staffUserId, "TICKET_SOLD", "tickets", ticketId,
                null, ticketInfo, ipAddress);
    }

    @Override
    public void logCustomerStatusUpdate(Integer adminUserId, Integer customerId, String oldStatus,
            String newStatus, String ipAddress) {
        logAction(adminUserId, "CUSTOMER_STATUS_UPDATED", "users", customerId,
                oldStatus, newStatus, ipAddress);
    }
}
