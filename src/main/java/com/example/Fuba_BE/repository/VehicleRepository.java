package com.example.Fuba_BE.repository;

import com.example.Fuba_BE.domain.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Integer> {
    List<Vehicle> findByStatus(String status);

    long countByStatus(String status);

    boolean existsByLicensePlate(String licensePlate);

    Page<Vehicle> findByLicensePlateContainingIgnoreCase(String licensePlate, Pageable pageable);
}
