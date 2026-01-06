package com.example.Fuba_BE.repository;

import com.example.Fuba_BE.domain.entity.Trip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
            "LEFT JOIN FETCH t.route r " +
            "LEFT JOIN FETCH r.origin " +
            "LEFT JOIN FETCH r.destination " +
            "LEFT JOIN FETCH t.vehicle v " +
            "LEFT JOIN FETCH v.vehicleType " +
            "LEFT JOIN FETCH t.driver d " +
            "LEFT JOIN FETCH d.user " +
            "WHERE t.departureTime >= :startOfDay AND t.departureTime <= :endOfDay " +
            "ORDER BY t.departureTime ASC")
    List<Trip> findAllTripsByDate(@Param("startOfDay") LocalDateTime startOfDay,
                                  @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT t FROM Trip t " +
            "JOIN t.route r " +
            "WHERE (:status IS NULL OR t.status = :status) " +
            "AND t.departureTime >= COALESCE(:startDate, t.departureTime) " +
            "AND t.departureTime <= COALESCE(:endDate, t.departureTime) " +
            "AND (:originId IS NULL OR r.origin.locationId = :originId) " +
            "AND (:destId IS NULL OR r.destination.locationId = :destId)")
    Page<Trip> findTripsWithFilter(@Param("status") String status,
                                   @Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate,
                                   @Param("originId") Integer originId,
                                   @Param("destId") Integer destId,
                                   Pageable pageable);

    @Modifying
    @Query("UPDATE Trip t SET t.status = :status WHERE t.tripId = :tripId")
    int updateStatus(@Param("tripId") Integer tripId, @Param("status") String status);

    @Query(value = """
        SELECT t FROM Trip t
        LEFT JOIN FETCH t.route r
        LEFT JOIN FETCH r.origin o
        LEFT JOIN FETCH r.destination d
        LEFT JOIN FETCH t.vehicle v
        LEFT JOIN FETCH v.vehicleType vt
        WHERE t.departureTime BETWEEN :start AND :end
    """,
            countQuery = """
        SELECT COUNT(t) FROM Trip t
        WHERE t.departureTime BETWEEN :start AND :end
    """)
    Page<Trip> findTripsByDate(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );

    @Query(value = """
        SELECT t, 
        (SELECT COUNT(ts) FROM TripSeat ts WHERE ts.trip = t AND (ts.status = 'Booked' OR ts.status = 'Held')) 
        FROM Trip t
        LEFT JOIN FETCH t.route r
        LEFT JOIN FETCH r.origin o
        LEFT JOIN FETCH r.destination d
        LEFT JOIN FETCH t.vehicle v
        LEFT JOIN FETCH v.vehicleType vt
        WHERE t.departureTime BETWEEN :start AND :end
    """,
            countQuery = """
        SELECT COUNT(t) FROM Trip t
        WHERE t.departureTime BETWEEN :start AND :end
    """)
    Page<Object[]> findTripsWithBookingCount(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );

    @Query(value = """
        SELECT TO_CHAR(t.departuretime, 'Dy'), COUNT(tk.ticketid)
        FROM tickets tk
        JOIN bookings b ON tk.bookingid = b.bookingid
        JOIN trips t ON b.tripid = t.tripid
        WHERE t.departuretime >= CURRENT_DATE - INTERVAL '6 days'
        AND tk.ticketstatus IN ('Confirmed', 'Used')
        GROUP BY TO_CHAR(t.departuretime, 'Dy'), DATE(t.departuretime)
        ORDER BY DATE(t.departuretime) ASC
    """, nativeQuery = true)
    List<Object[]> getWeeklyTicketSales();

    @Query("SELECT COUNT(t) > 0 FROM Trip t " +
            "WHERE t.vehicle.vehicleId = :vehicleId " +
            "AND t.status != 'Cancelled' " +
            "AND t.departureTime < :endTime " +
            "AND t.arrivalTime > :startTime")
    boolean existsByVehicleAndOverlap(@Param("vehicleId") Integer vehicleId,
                                      @Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime);

    @Query("SELECT COUNT(t) > 0 FROM Trip t " +
            "WHERE (t.driver.driverId = :personId OR t.subDriver.driverId = :personId)" +
            "AND t.status != 'Cancelled' " +
            "AND t.departureTime < :endTime " +
            "AND t.arrivalTime > :startTime")
    boolean isPersonBusy(@Param("personId") Integer personId,
                         @Param("startTime") LocalDateTime startTime,
                         @Param("endTime") LocalDateTime endTime);
}