package com.example.Fuba_BE.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicletypes")
@Data
@NoArgsConstructor
@AllArgsConstructor
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