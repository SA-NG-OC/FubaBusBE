package com.example.Fuba_BE.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "vehicletypes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "typeid")
    private Integer typeId;

    @Column(name = "typename", nullable = false, unique = true)
    private String typeName;

    @Column(name = "totalseats", nullable = false)
    private Integer totalSeats;

    @Column(name = "numberoffloors")
    @Builder.Default
    private Integer numberOfFloors = 1;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "createdat")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}