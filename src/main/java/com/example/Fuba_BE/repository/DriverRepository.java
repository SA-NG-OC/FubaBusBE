package com.example.Fuba_BE.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.Fuba_BE.domain.entity.Driver;

public interface DriverRepository extends JpaRepository<Driver, Integer> {
    @Query("SELECT COUNT(d) FROM Driver d WHERE d.licenseExpiry >= CURRENT_DATE")
    long countActiveDrivers();

    @Query("SELECT d FROM Driver d " +
            "JOIN FETCH d.user u " +
            "JOIN u.role r " +
            "WHERE r.roleName = 'DRIVER'") // Chỉ lấy role chuẩn DRIVER
    List<Driver> findAllWithUserAndRoleDriver();

    @Query("SELECT d FROM Driver d JOIN FETCH d.user u WHERE u.userId = :userId")
    Optional<Driver> findByUserId(@Param("userId") Integer userId);

    @Query("SELECT DISTINCT d FROM Driver d " +
            "JOIN FETCH d.user u " +
            "WHERE (:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(d.driverLicense) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.phoneNumber) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Driver> findAllWithUserAndKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT d FROM Driver d " +
            "JOIN FETCH d.user u " +
            "WHERE d.driverId = :id")
    Optional<Driver> findByIdWithUser(@Param("id") Integer id);
}
