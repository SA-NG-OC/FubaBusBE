package com.example.Fuba_BE.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.Fuba_BE.domain.entity.AuditLog;
import com.example.Fuba_BE.dto.AuditLog.AuditLogResponseDTO;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "userName", source = "user.fullName")
    @Mapping(target = "userEmail", source = "user.email")
    @Mapping(target = "userRole", source = "user.role.roleName")
    AuditLogResponseDTO toResponseDTO(AuditLog auditLog);
}
