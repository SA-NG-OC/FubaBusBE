package com.example.Fuba_BE.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vehicleid")
    private Integer vehicleId;

    @Column(name = "licenseplate", nullable = false, unique = true)
    private String licensePlate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "typeid", nullable = false)
    private VehicleType vehicleType;

    @Column(name = "insurancenumber")
    private String insuranceNumber;

    @Column(name = "insuranceexpiry")
    private LocalDate insuranceExpiry;

    @Column(name = "status")
    private String status = "Hoàn thiện";

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
}