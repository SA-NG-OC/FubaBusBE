package com.example.Fuba_BE.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.Fuba_BE.domain.entity.DriverRouteAssignment;

@Repository
public interface DriverRouteAssignmentRepository extends JpaRepository<DriverRouteAssignment, Integer> {

       /**
        * Find all assignments with pagination and eager loading
        */
       @Override
       @Query(value = "SELECT dra FROM DriverRouteAssignment dra " +
                     "LEFT JOIN FETCH dra.driver d " +
                     "LEFT JOIN FETCH d.user u " +
                     "LEFT JOIN FETCH dra.route r " +
                     "LEFT JOIN FETCH r.origin " +
                     "LEFT JOIN FETCH r.destination", countQuery = "SELECT COUNT(dra) FROM DriverRouteAssignment dra")
       Page<DriverRouteAssignment> findAll(Pageable pageable);

       /**
        * Find all active assignments for a driver
        */
       @Query("SELECT dra FROM DriverRouteAssignment dra " +
                     "LEFT JOIN FETCH dra.driver d " +
                     "LEFT JOIN FETCH dra.route r " +
                     "LEFT JOIN FETCH r.origin " +
                     "LEFT JOIN FETCH r.destination " +
                     "WHERE dra.driver.driverId = :driverId " +
                     "AND dra.isActive = true")
       List<DriverRouteAssignment> findActiveByDriverId(@Param("driverId") Integer driverId);

       /**
        * Find all active assignments for a route
        */
       @Query("SELECT dra FROM DriverRouteAssignment dra " +
                     "LEFT JOIN FETCH dra.driver d " +
                     "LEFT JOIN FETCH d.user u " +
                     "LEFT JOIN FETCH dra.route r " +
                     "LEFT JOIN FETCH r.origin " +
                     "LEFT JOIN FETCH r.destination " +
                     "WHERE dra.route.routeId = :routeId " +
                     "AND dra.isActive = true")
       List<DriverRouteAssignment> findActiveByRouteId(@Param("routeId") Integer routeId);

       /**
        * Find active assignments for route with specific role
        */
       @Query("SELECT dra FROM DriverRouteAssignment dra " +
                     "LEFT JOIN FETCH dra.driver d " +
                     "LEFT JOIN FETCH d.user u " +
                     "LEFT JOIN FETCH dra.route r " +
                     "LEFT JOIN FETCH r.origin " +
                     "LEFT JOIN FETCH r.destination " +
                     "WHERE dra.route.routeId = :routeId " +
                     "AND dra.preferredRole = :role " +
                     "AND dra.isActive = true " +
                     "ORDER BY dra.priority ASC")
       List<DriverRouteAssignment> findActiveByRouteAndRole(
                     @Param("routeId") Integer routeId,
                     @Param("role") String role);

       /**
        * Find currently effective assignments for route (considering start/end dates)
        */
       @Query("SELECT dra FROM DriverRouteAssignment dra " +
                     "LEFT JOIN FETCH dra.driver d " +
                     "LEFT JOIN FETCH d.user u " +
                     "LEFT JOIN FETCH dra.route r " +
                     "LEFT JOIN FETCH r.origin " +
                     "LEFT JOIN FETCH r.destination " +
                     "WHERE dra.route.routeId = :routeId " +
                     "AND dra.isActive = true " +
                     "AND (dra.startDate IS NULL OR dra.startDate <= :date) " +
                     "AND (dra.endDate IS NULL OR dra.endDate >= :date) " +
                     "ORDER BY dra.priority ASC")
       List<DriverRouteAssignment> findEffectiveByRouteAndDate(
                     @Param("routeId") Integer routeId,
                     @Param("date") LocalDate date);

       /**
        * Check if driver is already assigned to route
        */
       @Query("SELECT COUNT(dra) > 0 FROM DriverRouteAssignment dra " +
                     "WHERE dra.driver.driverId = :driverId " +
                     "AND dra.route.routeId = :routeId " +
                     "AND dra.preferredRole = :role " +
                     "AND dra.isActive = true")
       boolean existsByDriverAndRouteAndRole(
                     @Param("driverId") Integer driverId,
                     @Param("routeId") Integer routeId,
                     @Param("role") String role);

       /**
        * Find specific assignment
        */
       Optional<DriverRouteAssignment> findByDriverDriverIdAndRouteRouteIdAndPreferredRole(
                     Integer driverId,
                     Integer routeId,
                     String preferredRole);

       /**
        * Deactivate all assignments for a driver
        */
       @Query("UPDATE DriverRouteAssignment dra SET dra.isActive = false " +
                     "WHERE dra.driver.driverId = :driverId")
       void deactivateAllForDriver(@Param("driverId") Integer driverId);

       /**
        * Find all assignments by driver with JOIN FETCH
        */
       @Query("SELECT dra FROM DriverRouteAssignment dra " +
                     "LEFT JOIN FETCH dra.driver d " +
                     "LEFT JOIN FETCH d.user u " +
                     "LEFT JOIN FETCH dra.route r " +
                     "LEFT JOIN FETCH r.origin " +
                     "LEFT JOIN FETCH r.destination " +
                     "WHERE dra.driver.driverId = :driverId " +
                     "ORDER BY dra.priority ASC")
       List<DriverRouteAssignment> findByDriverDriverId(@Param("driverId") Integer driverId);

       /**
        * Find all assignments by route ordered by priority with JOIN FETCH
        */
       @Query("SELECT dra FROM DriverRouteAssignment dra " +
                     "LEFT JOIN FETCH dra.driver d " +
                     "LEFT JOIN FETCH d.user u " +
                     "LEFT JOIN FETCH dra.route r " +
                     "LEFT JOIN FETCH r.origin " +
                     "LEFT JOIN FETCH r.destination " +
                     "WHERE dra.route.routeId = :routeId " +
                     "ORDER BY dra.priority ASC")
       List<DriverRouteAssignment> findByRouteRouteIdOrderByPriorityAsc(@Param("routeId") Integer routeId);

       /**
        * Check for overlapping assignments
        */
       @Query("SELECT COUNT(dra) > 0 FROM DriverRouteAssignment dra " +
                     "WHERE dra.driver.driverId = :driverId " +
                     "AND dra.route.routeId = :routeId " +
                     "AND dra.isActive = true " +
                     "AND (" +
                     "  (dra.startDate <= :endDate AND (dra.endDate IS NULL OR dra.endDate >= :startDate))" +
                     ")")
       boolean hasOverlappingAssignment(
                     @Param("driverId") Integer driverId,
                     @Param("routeId") Integer routeId,
                     @Param("startDate") LocalDate startDate,
                     @Param("endDate") LocalDate endDate);

       /**
        * Find assignments by driver and date
        */
       @Query("SELECT dra FROM DriverRouteAssignment dra " +
                     "LEFT JOIN FETCH dra.driver d " +
                     "LEFT JOIN FETCH dra.route r " +
                     "LEFT JOIN FETCH r.origin " +
                     "LEFT JOIN FETCH r.destination " +
                     "WHERE dra.driver.driverId = :driverId " +
                     "AND dra.isActive = true " +
                     "AND (dra.startDate IS NULL OR dra.startDate <= :date) " +
                     "AND (dra.endDate IS NULL OR dra.endDate >= :date)")
       List<DriverRouteAssignment> findByDriverIdAndDate(
                     @Param("driverId") Integer driverId,
                     @Param("date") LocalDate date);

       /**
        * Find assignment by ID with all relationships loaded
        */
       @Query("SELECT dra FROM DriverRouteAssignment dra " +
                     "LEFT JOIN FETCH dra.driver d " +
                     "LEFT JOIN FETCH d.user u " +
                     "LEFT JOIN FETCH dra.route r " +
                     "LEFT JOIN FETCH r.origin " +
                     "LEFT JOIN FETCH r.destination " +
                     "WHERE dra.assignmentId = :id")
       Optional<DriverRouteAssignment> findByIdWithRelations(@Param("id") Integer id);
}
