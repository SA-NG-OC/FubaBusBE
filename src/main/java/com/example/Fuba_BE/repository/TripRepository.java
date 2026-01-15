package com.example.Fuba_BE.repository;

import com.example.Fuba_BE.domain.entity.Trip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Integer>, JpaSpecificationExecutor<Trip> {

    // =========================================================================
    // PHẦN 1: KHÔI PHỤC CÁC HÀM CŨ (ĐỂ FIX LỖI BUILD ANALYTICS & DASHBOARD)
    // =========================================================================

    // 1. Hàm cho AnalyticsService (Dòng 141 bị lỗi)
    @Query("SELECT SUM(vt.totalSeats) FROM Trip t " +
            "JOIN t.vehicle v " +
            "JOIN v.vehicleType vt " +
            "WHERE t.departureTime >= :start AND t.departureTime <= :end " +
            "AND t.status <> 'Cancelled'")
    Long sumTotalCapacityBetween(@Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);

    // 2. Hàm cho DashboardService (Dòng 78 bị lỗi - Chart tuần)
    @Query(value = """
        SELECT TO_CHAR(t.departuretime, 'Dy'), COUNT(tk.ticketid)
        FROM tickets tk
        JOIN bookings b ON tk.bookingid = b.bookingid
        JOIN trips t ON b.tripid = t.tripid
        WHERE t.departuretime >= CURRENT_DATE - INTERVAL '7 days'
        AND tk.ticketstatus IN ('Confirmed', 'USED')
        GROUP BY TO_CHAR(t.departuretime, 'Dy'), DATE(t.departuretime)
        ORDER BY DATE(t.departuretime) ASC
    """, nativeQuery = true)
    List<Object[]> getWeeklyTicketSales();

    // 3. Hàm cho DashboardService (Dòng 98 bị lỗi - List hôm nay)
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
    """, countQuery = """
        SELECT COUNT(t) FROM Trip t
        WHERE t.departureTime BETWEEN :start AND :end
    """)
    Page<Object[]> findTripsWithBookingCount(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );

    // =========================================================================
    // PHẦN 2: CÁC HÀM ĐẾM SỐ LIỆU CHO TRIP CARD (QUAN TRỌNG ĐỂ FIX LỖI 0)
    // =========================================================================

    // Đếm Booked (Chấp nhận mọi kiểu viết hoa thường: Booked, BOOKED, booked...)
    @Query("SELECT COUNT(ts) FROM TripSeat ts WHERE ts.trip.tripId = :tripId AND LOWER(ts.status) IN ('booked', 'sold', 'reserved', 'paid')")
    int countBookedSeats(@Param("tripId") Integer tripId);

    // Đếm Checked-in
    @Query("SELECT COUNT(ts) FROM TripSeat ts WHERE ts.trip.tripId = :tripId AND LOWER(ts.status) IN ('checkedin', 'used', 'checked-in')")
    int countCheckedInSeats(@Param("tripId") Integer tripId);

    // =========================================================================
    // PHẦN 3: CÁC HÀM CƠ BẢN & FILTER (ĐÃ FIX LAZY LOAD)
    // =========================================================================

    @Query("SELECT DISTINCT CAST(t.departureTime AS LocalDate) FROM Trip t WHERE t.departureTime >= :start AND t.departureTime <= :end")
    List<LocalDate> findDistinctTripDates(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT t FROM Trip t LEFT JOIN FETCH t.route r LEFT JOIN FETCH r.origin LEFT JOIN FETCH r.destination LEFT JOIN FETCH t.vehicle v LEFT JOIN FETCH v.vehicleType LEFT JOIN FETCH t.driver d LEFT JOIN FETCH d.user WHERE t.departureTime >= :startOfDay AND t.departureTime <= :endOfDay ORDER BY t.departureTime ASC")
    List<Trip> findAllTripsByDate(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    // Query Filter "Thần thánh" fix lỗi Lazy
    @Query(value = "SELECT t FROM Trip t " +
            "JOIN FETCH t.route r " +
            "JOIN FETCH r.origin " +
            "JOIN FETCH r.destination " +
            "LEFT JOIN FETCH t.driver d " +
            "LEFT JOIN FETCH d.user " +
            "LEFT JOIN FETCH t.subDriver sd " +
            "LEFT JOIN FETCH sd.user " +
            "LEFT JOIN FETCH t.vehicle v " +
            "LEFT JOIN FETCH v.vehicleType " +
            "WHERE (CAST(:status AS string) IS NULL OR t.status = :status) " +
            "AND (CAST(:start AS timestamp) IS NULL OR t.departureTime >= :start) " +
            "AND (CAST(:end AS timestamp) IS NULL OR t.departureTime <= :end) " +
            "AND (CAST(:originId AS integer) IS NULL OR r.origin.locationId = :originId) " +
            "AND (CAST(:destId AS integer) IS NULL OR r.destination.locationId = :destId)",
            countQuery = "SELECT COUNT(t) FROM Trip t " +
                    "JOIN t.route r " +
                    "WHERE (CAST(:status AS string) IS NULL OR t.status = :status) " +
                    "AND (CAST(:start AS timestamp) IS NULL OR t.departureTime >= :start) " +
                    "AND (CAST(:end AS timestamp) IS NULL OR t.departureTime <= :end) " +
                    "AND (CAST(:originId AS integer) IS NULL OR r.origin.locationId = :originId) " +
                    "AND (CAST(:destId AS integer) IS NULL OR r.destination.locationId = :destId)")
    Page<Trip> findTripsWithFilter(@Param("status") String status, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("originId") Integer originId, @Param("destId") Integer destId, Pageable pageable);

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
    """, countQuery = """
        SELECT COUNT(t) FROM Trip t
        WHERE t.departureTime BETWEEN :start AND :end
    """)
    Page<Trip> findTripsByDate(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    // =========================================================================
    // PHẦN 4: CHECK TRÙNG & ANALYTICS CÒN LẠI
    // =========================================================================

    @Query("SELECT COUNT(t) > 0 FROM Trip t WHERE t.vehicle.vehicleId = :vehicleId AND t.status != 'Cancelled' AND ((t.departureTime < :endTime) AND (t.arrivalTime > :startTime))")
    boolean existsByVehicleAndOverlap(@Param("vehicleId") Integer vehicleId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT COUNT(t) > 0 FROM Trip t WHERE (t.driver.driverId = :personId OR t.subDriver.driverId = :personId) AND t.status != 'Cancelled' AND ((t.departureTime < :endTime) AND (t.arrivalTime > :startTime))")
    boolean isPersonBusy(@Param("personId") Integer personId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT t FROM Trip t WHERE (t.driver.driverId = :driverId OR t.subDriver.driverId = :driverId) AND (:status IS NULL OR t.status = :status)")
    Page<Trip> findTripsByDriverOrSubDriver(@Param("driverId") Integer driverId, @Param("status") String status, Pageable pageable);

    long countByDepartureTimeBetween(LocalDateTime start, LocalDateTime end);
    long countByDepartureTimeBetweenAndStatus(LocalDateTime start, LocalDateTime end, String status);

    @Query("SELECT COALESCE(SUM(t.basePrice), 0) FROM Trip t WHERE t.departureTime BETWEEN :start AND :end AND t.status != 'Cancelled'")
    BigDecimal sumRevenueByTimeRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(t) FROM Trip t WHERE t.departureTime BETWEEN :start AND :end AND (:routeId IS NULL OR t.route.routeId = :routeId)")
    long countTrips(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("routeId") Integer routeId);

    @Query("SELECT COUNT(t) FROM Trip t WHERE t.departureTime BETWEEN :start AND :end AND t.status = :status AND (:routeId IS NULL OR t.route.routeId = :routeId)")
    long countTripsByStatus(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("status") String status, @Param("routeId") Integer routeId);

    @Query("SELECT COALESCE(SUM(t.basePrice), 0) FROM Trip t WHERE t.departureTime BETWEEN :start AND :end AND t.status != 'Cancelled' AND (:routeId IS NULL OR t.route.routeId = :routeId)")
    BigDecimal sumTripBasePrice(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("routeId") Integer routeId);

    @Query("SELECT COUNT(ts) FROM TripSeat ts JOIN ts.trip t WHERE t.departureTime BETWEEN :start AND :end AND t.status != 'Cancelled' AND (:routeId IS NULL OR t.route.routeId = :routeId)")
    long countTotalSeats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("routeId") Integer routeId);

    @Query("SELECT COUNT(ts) FROM TripSeat ts JOIN ts.trip t WHERE t.departureTime BETWEEN :start AND :end AND t.status != 'Cancelled' AND ts.status = 'Booked' AND (:routeId IS NULL OR t.route.routeId = :routeId)")
    long countSoldTickets(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("routeId") Integer routeId);

    @Query("SELECT COALESCE(SUM(t.basePrice), 0) FROM TripSeat ts JOIN ts.trip t WHERE t.departureTime BETWEEN :start AND :end AND t.status != 'Cancelled' AND ts.status = 'Booked' AND (:routeId IS NULL OR t.route.routeId = :routeId)")
    BigDecimal sumTicketRevenue(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("routeId") Integer routeId);
}