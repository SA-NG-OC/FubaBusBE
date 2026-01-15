package com.example.Fuba_BE.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.Fuba_BE.domain.entity.VehicleRouteAssignment;

@Repository
public interface VehicleRouteAssignmentRepository extends JpaRepository<VehicleRouteAssignment, Integer> {

    /**
     * Find all active assignments for a vehicle
     */
    @Query("SELECT vra FROM VehicleRouteAssignment vra " +
           "LEFT JOIN FETCH vra.vehicle v " +
           "LEFT JOIN FETCH vra.route r " +
           "WHERE vra.vehicle.vehicleId = :vehicleId " +
           "AND vra.isActive = true")
    List<VehicleRouteAssignment> findActiveByVehicleId(@Param("vehicleId") Integer vehicleId);

    /**
     * Find all active assignments for a route
     */
    @Query("SELECT vra FROM VehicleRouteAssignment vra " +
           "LEFT JOIN FETCH vra.vehicle v " +
           "LEFT JOIN FETCH v.vehicleType vt " +
           "LEFT JOIN FETCH vra.route r " +
           "WHERE vra.route.routeId = :routeId " +
           "AND vra.isActive = true " +
           "ORDER BY vra.priority ASC")
    List<VehicleRouteAssignment> findActiveByRouteId(@Param("routeId") Integer routeId);

    /**
     * Find currently effective assignments for route (considering start/end dates)
     */
    @Query("SELECT vra FROM VehicleRouteAssignment vra " +
           "LEFT JOIN FETCH vra.vehicle v " +
           "LEFT JOIN FETCH v.vehicleType vt " +
           "WHERE vra.route.routeId = :routeId " +
           "AND vra.isActive = true " +
           "AND (vra.startDate IS NULL OR vra.startDate <= :date) " +
           "AND (vra.endDate IS NULL OR vra.endDate >= :date) " +
           "ORDER BY vra.priority ASC")
    List<VehicleRouteAssignment> findEffectiveByRouteAndDate(
            @Param("routeId") Integer routeId,
            @Param("date") LocalDate date
    );

    /**
     * Find vehicles needing maintenance
     */
    @Query("SELECT vra FROM VehicleRouteAssignment vra " +
           "LEFT JOIN FETCH vra.vehicle v " +
           "WHERE vra.isActive = true " +
           "AND vra.nextMaintenanceDate IS NOT NULL " +
           "AND vra.nextMaintenanceDate <= :date")
    List<VehicleRouteAssignment> findVehiclesNeedingMaintenance(@Param("date") LocalDate date);

    /**
     * Check if vehicle is already assigned to route
     */
    @Query("SELECT COUNT(vra) > 0 FROM VehicleRouteAssignment vra " +
           "WHERE vra.vehicle.vehicleId = :vehicleId " +
           "AND vra.route.routeId = :routeId " +
           "AND vra.isActive = true")
    boolean existsByVehicleAndRoute(
            @Param("vehicleId") Integer vehicleId,
            @Param("routeId") Integer routeId
    );

    /**
     * Find specific assignment
     */
    Optional<VehicleRouteAssignment> findByVehicleVehicleIdAndRouteRouteId(
            Integer vehicleId,
            Integer routeId
    );

    /**
     * Deactivate all assignments for a vehicle
     */
    @Query("UPDATE VehicleRouteAssignment vra SET vra.isActive = false " +
           "WHERE vra.vehicle.vehicleId = :vehicleId")
    void deactivateAllForVehicle(@Param("vehicleId") Integer vehicleId);

    /**
     * Find all assignments by vehicle (Spring Data method)
     */
    List<VehicleRouteAssignment> findByVehicleVehicleId(Integer vehicleId);

    /**
     * Find all assignments by route ordered by priority (Spring Data method)
     */
    List<VehicleRouteAssignment> findByRouteRouteIdOrderByPriorityAsc(Integer routeId);

    /**
     * Find vehicles needing maintenance
     */
    @Query("SELECT vra FROM VehicleRouteAssignment vra " +
           "LEFT JOIN FETCH vra.vehicle v " +
           "LEFT JOIN FETCH vra.route r " +
           "WHERE vra.isActive = true " +
           "AND vra.nextMaintenanceDate IS NOT NULL " +
           "AND vra.nextMaintenanceDate <= :date")
    List<VehicleRouteAssignment> findByNextMaintenanceDateBeforeOrEqual(@Param("date") LocalDate date);

    /**
     * Check for overlapping assignments
     */
    @Query("SELECT COUNT(vra) > 0 FROM VehicleRouteAssignment vra " +
           "WHERE vra.vehicle.vehicleId = :vehicleId " +
           "AND vra.route.routeId = :routeId " +
           "AND vra.isActive = true " +
           "AND (" +
           "  (vra.startDate <= :endDate AND (vra.endDate IS NULL OR vra.endDate >= :startDate))" +
           ")")
    boolean hasOverlappingAssignment(
            @Param("vehicleId") Integer vehicleId,
            @Param("routeId") Integer routeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Find assignments by vehicle and date
     */
    @Query("SELECT vra FROM VehicleRouteAssignment vra " +
           "LEFT JOIN FETCH vra.vehicle v " +
           "LEFT JOIN FETCH vra.route r " +
           "WHERE vra.vehicle.vehicleId = :vehicleId " +
           "AND vra.isActive = true " +
           "AND (vra.startDate IS NULL OR vra.startDate <= :date) " +
           "AND (vra.endDate IS NULL OR vra.endDate >= :date)")
    List<VehicleRouteAssignment> findByVehicleIdAndDate(
            @Param("vehicleId") Integer vehicleId,
            @Param("date") LocalDate date
    );
}

