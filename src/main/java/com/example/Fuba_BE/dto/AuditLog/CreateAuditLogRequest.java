package com.example.Fuba_BE.dto.AuditLog;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAuditLogRequest {

    @NotBlank(message = "Action is required")
    private String action;

    private String tableName;

    private Integer recordId;

    private String oldValue;

    private String newValue;

    private String ipAddress;
}
