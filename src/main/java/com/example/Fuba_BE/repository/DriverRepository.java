package com.example.Fuba_BE.repository;

import com.example.Fuba_BE.domain.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverRepository extends JpaRepository<Driver, Integer> {
}
