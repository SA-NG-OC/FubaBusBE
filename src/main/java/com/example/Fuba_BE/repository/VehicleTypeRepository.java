package com.example.Fuba_BE.repository;

import com.example.Fuba_BE.domain.entity.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleTypeRepository extends JpaRepository<VehicleType, Integer> {
}
