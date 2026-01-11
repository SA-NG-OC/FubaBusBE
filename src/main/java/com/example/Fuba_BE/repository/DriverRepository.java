package com.example.Fuba_BE.repository;

import com.example.Fuba_BE.domain.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DriverRepository extends JpaRepository<Driver, Integer> {
    @Query("SELECT COUNT(d) FROM Driver d WHERE d.licenseExpiry >= CURRENT_DATE")
    long countActiveDrivers();

    @Query("SELECT d FROM Driver d JOIN FETCH d.user")
    List<Driver> findAllWithUser();
}
