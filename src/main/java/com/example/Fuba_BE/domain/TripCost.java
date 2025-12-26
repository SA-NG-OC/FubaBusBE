package com.example.Fuba_BE.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tripcosts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripCost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "costid")
    private Integer costId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tripid", nullable = false, unique = true)
    private Trip trip;

    @Column(name = "fuelcost")
    private BigDecimal fuelCost = BigDecimal.ZERO;

    @Column(name = "tollfeecost")
    private BigDecimal tollFeeCost = BigDecimal.ZERO;

    @Column(name = "driversalary")
    private BigDecimal driverSalary = BigDecimal.ZERO;

    @Column(name = "maintenancecost")
    private BigDecimal maintenanceCost = BigDecimal.ZERO;

    @Column(name = "insurancecost")
    private BigDecimal insuranceCost = BigDecimal.ZERO;

    @Column(name = "parkingcost")
    private BigDecimal parkingCost = BigDecimal.ZERO;

    @Column(name = "servicecost")
    private BigDecimal serviceCost = BigDecimal.ZERO;

    @Column(name = "othercosts")
    private BigDecimal otherCosts = BigDecimal.ZERO;

    @Column(name = "revenue")
    private BigDecimal revenue = BigDecimal.ZERO;

    @Column(name = "cancelledrevenue")
    private BigDecimal cancelledRevenue = BigDecimal.ZERO;

    @Column(name = "profitmargin")
    private BigDecimal profitMargin;

    @Column(name = "costnote", columnDefinition = "TEXT")
    private String costNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calculatedby")
    private User calculatedBy;

    @Column(name = "calculatedat")
    private LocalDateTime calculatedAt;

    @Column(name = "updatedat")
    private LocalDateTime updatedAt;

    @Column(name = "totalcost")
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Column(name = "netrevenue")
    private BigDecimal netRevenue = BigDecimal.ZERO;

    @Column(name = "profit")
    private BigDecimal profit = BigDecimal.ZERO;

    @PrePersist
    protected void onCreate() {
        calculatedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}