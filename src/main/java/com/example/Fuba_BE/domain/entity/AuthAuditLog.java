package com.example.Fuba_BE.domain.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "auth_audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "logid")
    private Long logId;

    @Column(name = "userid")
    private Integer userId;

    @Column(name = "email")
    private String email;

    @Column(name = "phonenumber")
    private String phoneNumber;

    @Column(name = "action", nullable = false)
    private String action; // LOGIN, REGISTER, LOGOUT, PASSWORD_RESET_REQUEST, PASSWORD_RESET, LOGIN_FAILED, ACCOUNT_LOCKED

    @Column(name = "status", nullable = false)
    private String status; // SUCCESS, FAILURE

    @Column(name = "ipaddress")
    private String ipAddress;

    @Column(name = "useragent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "createdat", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}
