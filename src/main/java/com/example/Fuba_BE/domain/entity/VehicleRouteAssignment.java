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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "vehiclerouteassignments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleRouteAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignmentid")
    private Integer assignmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicleid", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routeid", nullable = false)
    private Route route;

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 1;

    @Column(name = "isactive")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "startdate")
    private LocalDate startDate;

    @Column(name = "enddate")
    private LocalDate endDate;

    @Column(name = "maintenanceschedule")
    private String maintenanceSchedule; // "Weekly", "Bi-weekly", "Monthly", "Quarterly"

    @Column(name = "lastmaintenancedate")
    private LocalDate lastMaintenanceDate;

    @Column(name = "nextmaintenancedate")
    private LocalDate nextMaintenanceDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "createdat")
    private LocalDateTime createdAt;

    @Column(name = "updatedat")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if assignment is currently effective
     */
    public boolean isCurrentlyEffective() {
        if (!isActive) {
            return false;
        }
        
        LocalDate today = LocalDate.now();
        
        if (startDate != null && today.isBefore(startDate)) {
            return false;
        }
        
        if (endDate != null && today.isAfter(endDate)) {
            return false;
        }
        
        return true;
    }

    /**
     * Check if vehicle needs maintenance
     */
    public boolean needsMaintenance() {
        if (nextMaintenanceDate == null) {
            return false;
        }
        return LocalDate.now().isAfter(nextMaintenanceDate) || 
               LocalDate.now().isEqual(nextMaintenanceDate);
    }
}
