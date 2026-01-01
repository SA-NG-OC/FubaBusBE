package com.example.Fuba_BE.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "auditlogs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "logid")
    private Integer logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userid")
    private User user;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "tablename")
    private String tableName;

    @Column(name = "recordid")
    private Integer recordId;

    @Column(name = "oldvalue", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "newvalue", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "ipaddress")
    private String ipAddress;

    @Column(name = "createdat")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}