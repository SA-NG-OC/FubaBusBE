package com.example.Fuba_BE.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.Fuba_BE.domain.entity.Vehicle;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Integer> {

    List<Vehicle> findByStatus(String status);

    long countByStatus(String status);

    long countByStatusIgnoreCase(String status);

    boolean existsByLicensePlate(String licensePlate);

    /**
     * Check if license plate exists excluding current vehicle ID (for update)
     */
    boolean existsByLicensePlateAndVehicleIdNot(String licensePlate, Integer vehicleId);

    /**
     * Find vehicles with pagination - avoid N+1 by using JOIN FETCH
     */
    @Override
    @Query("SELECT DISTINCT v FROM Vehicle v LEFT JOIN FETCH v.vehicleType")
    Page<Vehicle> findAll(Pageable pageable);

    /**
     * Find by ID with vehicle type fetched - avoid N+1
     */
    @Query("SELECT v FROM Vehicle v LEFT JOIN FETCH v.vehicleType WHERE v.vehicleId = :id")
    Optional<Vehicle> findByIdWithVehicleType(@Param("id") Integer id);

    /**
     * Search by license plate with pagination - avoid N+1
     */
    @Query("SELECT DISTINCT v FROM Vehicle v LEFT JOIN FETCH v.vehicleType WHERE LOWER(v.licensePlate) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Vehicle> findByLicensePlateContainingIgnoreCase(@Param("keyword") String licensePlate, Pageable pageable);

    @Query("SELECT v FROM Vehicle v JOIN FETCH v.vehicleType")
    List<Vehicle> findAllWithVehicleType();

    @Query("SELECT DISTINCT v FROM Vehicle v LEFT JOIN FETCH v.vehicleType WHERE LOWER(v.status) = LOWER(:status)")
    Page<Vehicle> findByStatusIgnoreCase(@Param("status") String status, Pageable pageable);

    @Query("SELECT DISTINCT v FROM Vehicle v LEFT JOIN FETCH v.vehicleType " +
            "WHERE LOWER(v.licensePlate) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "AND LOWER(v.status) = LOWER(:status)")
    Page<Vehicle> findByKeywordAndStatus(@Param("keyword") String keyword, @Param("status") String status,
            Pageable pageable);

    @Query("SELECT DISTINCT v FROM Vehicle v " +
            "LEFT JOIN FETCH v.vehicleType vt " +
            "JOIN VehicleRouteAssignment vra ON vra.vehicle = v " +
            "WHERE vra.route.routeId = :routeId " +
            "AND (:keyword IS NULL OR :keyword = '' OR LOWER(v.licensePlate) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:status IS NULL OR :status = '' OR LOWER(v.status) = LOWER(:status))")
    Page<Vehicle> findByRouteId(@Param("routeId") Integer routeId, @Param("keyword") String keyword,
            @Param("status") String status, Pageable pageable);
}
