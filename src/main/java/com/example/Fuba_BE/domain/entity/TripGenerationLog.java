package com.example.Fuba_BE.domain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tripgenerationlogs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripGenerationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "logid")
    private Integer logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "templateid", nullable = false)
    private TripTemplate template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generatedby")
    private User generatedBy;

    @Column(name = "startdate", nullable = false)
    private LocalDate startDate;

    @Column(name = "enddate", nullable = false)
    private LocalDate endDate;

    @Column(name = "totaltripscreated")
    @Builder.Default
    private Integer totalTripsCreated = 0;

    @Column(name = "totaltripsskipped")
    @Builder.Default
    private Integer totalTripsSkipped = 0;

    @Column(name = "skipreasons", columnDefinition = "TEXT")
    private String skipReasons; // JSON format: [{"date": "2026-01-15", "reason": "No available driver"}]

    @Column(name = "executiontime")
    private Integer executionTime; // Milliseconds

    @Column(name = "status")
    @Builder.Default
    private String status = "Success"; // "Success", "Partial", "Failed"

    @Column(name = "errormessage", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "createdat")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Calculate success rate
     */
    public double getSuccessRate() {
        int total = totalTripsCreated + totalTripsSkipped;
        if (total == 0) {
            return 0.0;
        }
        return (double) totalTripsCreated / total * 100;
    }
}
