package com.example.Fuba_BE.dto.AuditLog;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponseDTO {

    private Integer logId;
    private Integer userId;
    private String userName;
    private String userEmail;
    private String userRole;
    private String action;
    private String tableName;
    private Integer recordId;
    private String oldValue;
    private String newValue;
    private String ipAddress;
    private LocalDateTime createdAt;
}
