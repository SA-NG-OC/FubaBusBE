package com.example.Fuba_BE.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.Fuba_BE.domain.entity.TripTemplate;

@Repository
public interface TripTemplateRepository extends JpaRepository<TripTemplate, Integer> {

    /**
     * Find all active templates
     */
    @Query("SELECT tt FROM TripTemplate tt " +
           "LEFT JOIN FETCH tt.route r " +
           "WHERE tt.isActive = true")
    List<TripTemplate> findAllActive();

    /**
     * Find active templates for a route
     */
    @Query("SELECT tt FROM TripTemplate tt " +
           "LEFT JOIN FETCH tt.route r " +
           "WHERE tt.route.routeId = :routeId " +
           "AND tt.isActive = true")
    List<TripTemplate> findActiveByRouteId(@Param("routeId") Integer routeId);

    /**
     * Find currently effective templates (within date range)
     */
    @Query("SELECT tt FROM TripTemplate tt " +
           "LEFT JOIN FETCH tt.route r " +
           "WHERE tt.isActive = true " +
           "AND tt.effectiveFrom <= :date " +
           "AND (tt.effectiveTo IS NULL OR tt.effectiveTo >= :date)")
    List<TripTemplate> findEffectiveOnDate(@Param("date") LocalDate date);

    /**
     * Find effective templates for a specific route and date
     */
    @Query("SELECT tt FROM TripTemplate tt " +
           "LEFT JOIN FETCH tt.route r " +
           "WHERE tt.route.routeId = :routeId " +
           "AND tt.isActive = true " +
           "AND tt.effectiveFrom <= :date " +
           "AND (tt.effectiveTo IS NULL OR tt.effectiveTo >= :date)")
    List<TripTemplate> findEffectiveByRouteAndDate(
            @Param("routeId") Integer routeId,
            @Param("date") LocalDate date
    );

    /**
     * Check for duplicate template (same route + departure time + days)
     */
    @Query("SELECT COUNT(tt) > 0 FROM TripTemplate tt " +
           "WHERE tt.route.routeId = :routeId " +
           "AND tt.departureTime = :departureTime " +
           "AND tt.daysOfWeek = :daysOfWeek " +
           "AND tt.isActive = true " +
           "AND tt.effectiveFrom = :effectiveFrom " +
           "AND (:templateId IS NULL OR tt.templateId <> :templateId)")
    boolean existsDuplicateTemplate(
            @Param("routeId") Integer routeId,
            @Param("departureTime") LocalTime departureTime,
            @Param("daysOfWeek") String daysOfWeek,
            @Param("effectiveFrom") LocalDate effectiveFrom,
            @Param("templateId") Integer templateId
    );

    /**
     * Find template by ID with route loaded
     */
    @Query("SELECT tt FROM TripTemplate tt " +
           "LEFT JOIN FETCH tt.route r " +
           "LEFT JOIN FETCH r.origin " +
           "LEFT JOIN FETCH r.destination " +
           "WHERE tt.templateId = :templateId")
    Optional<TripTemplate> findByIdWithRoute(@Param("templateId") Integer templateId);

    /**
     * Find templates expiring soon
     */
    @Query("SELECT tt FROM TripTemplate tt " +
           "LEFT JOIN FETCH tt.route r " +
           "WHERE tt.isActive = true " +
           "AND tt.effectiveTo IS NOT NULL " +
           "AND tt.effectiveTo BETWEEN :startDate AND :endDate")
    List<TripTemplate> findExpiringSoon(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
