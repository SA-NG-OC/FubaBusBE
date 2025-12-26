package com.example.Fuba_BE.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "driverworklog")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverWorkLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "logid")
    private Integer logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driverid", nullable = false)
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tripid")
    private Trip trip;

    @Column(name = "workdate", nullable = false)
    private LocalDate workDate;

    @Column(name = "starttime", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "endtime")
    private LocalDateTime endTime;

    @Column(name = "totalhours")
    private BigDecimal totalHours;

    @Column(name = "breaktime")
    private BigDecimal breakTime = BigDecimal.ZERO;

    @Column(name = "tripcount")
    private Integer tripCount = 0;

    @Column(name = "totaldistance")
    private BigDecimal totalDistance = BigDecimal.ZERO;

    @Column(name = "salarytype")
    private String salaryType = "Theo chuyến";

    @Column(name = "salaryamount")
    private BigDecimal salaryAmount;

    @Column(name = "bonusamount")
    private BigDecimal bonusAmount = BigDecimal.ZERO;

    @Column(name = "penaltyamount")
    private BigDecimal penaltyAmount = BigDecimal.ZERO;

    @Column(name = "status")
    private String status = "Đang làm việc";

    @Column(name = "hasviolation")
    private Boolean hasViolation = false;

    @Column(name = "violationtype")
    private String violationType;

    @Column(name = "violationnote", columnDefinition = "TEXT")
    private String violationNote;

    @Column(name = "performancerating")
    private Integer performanceRating;

    @Column(name = "performancenote", columnDefinition = "TEXT")
    private String performanceNote;

    @Column(name = "createdat")
    private LocalDateTime createdAt;

    @Column(name = "updatedat")
    private LocalDateTime updatedAt;

    @Column(name = "totalsalary")
    private BigDecimal totalSalary = BigDecimal.ZERO;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}