package com.example.Fuba_BE.repository;

import com.example.Fuba_BE.domain.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Integer> {
    @Query("SELECT DISTINCT CAST(t.departureTime AS LocalDate) " +
            "FROM Trip t " +
            "WHERE t.departureTime >= :start AND t.departureTime <= :end")
    List<LocalDate> findDistinctTripDates(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    @Query("SELECT t FROM Trip t " +
            "JOIN FETCH t.route r " +
            "JOIN FETCH r.origin " +
            "JOIN FETCH r.destination " +
            "JOIN FETCH t.vehicle v " +
            "JOIN FETCH v.vehicleType " +
            "JOIN FETCH t.driver d " +
            "JOIN FETCH d.user " +
            "WHERE t.departureTime >= :startOfDay AND t.departureTime <= :endOfDay " +
            "ORDER BY t.departureTime ASC")
    List<Trip> findAllTripsByDate(@Param("startOfDay") LocalDateTime startOfDay,
                                  @Param("endOfDay") LocalDateTime endOfDay);
}
