package com.example.Fuba_BE.repository;

import com.example.Fuba_BE.domain.entity.Booking;
import com.example.Fuba_BE.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {

    // 1. Tính tổng doanh thu GROSS trong khoảng thời gian (Status: PAID, COMPLETED)
    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b " +
            "WHERE b.bookingStatus IN ('Paid', 'Completed') " +
            "AND b.createdAt BETWEEN :start AND :end")
    BigDecimal sumGrossRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 2. Tính tổng doanh thu NET (GROSS - Refund) trong khoảng thời gian
    @Query(value = """
        SELECT COALESCE(
            (SELECT SUM(totalamount) FROM bookings 
             WHERE bookingstatus IN ('Paid', 'Completed') 
             AND createdat BETWEEN :start AND :end), 0)
        - COALESCE(
            (SELECT SUM(refundamount) FROM refunds 
             WHERE refundstatus = 'Refunded' 
             AND createdat BETWEEN :start AND :end), 0)
        """, nativeQuery = true)
    BigDecimal sumRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // [THAY ĐỔI] Lấy doanh thu NET 12 tháng gần nhất (Gross - Refunds)
    // Sắp xếp theo Năm-Tháng để biểu đồ hiển thị đúng thứ tự thời gian
    @Query(value = """
        SELECT 
            months.month_label,
            COALESCE(booking_rev.gross_revenue, 0) - COALESCE(refund_amt.refund_total, 0) AS net_revenue
        FROM (
            SELECT TO_CHAR(DATE_TRUNC('month', CURRENT_DATE - (n || ' months')::INTERVAL), 'Mon-YY') AS month_label,
                   DATE_TRUNC('month', CURRENT_DATE - (n || ' months')::INTERVAL) AS month_start
            FROM generate_series(11, 0, -1) AS n
        ) months
        LEFT JOIN (
            SELECT DATE_TRUNC('month', createdat) AS month_start, SUM(totalamount) AS gross_revenue
            FROM bookings
            WHERE bookingstatus IN ('Paid', 'Completed')
            AND createdat >= DATE_TRUNC('month', CURRENT_DATE - INTERVAL '11 months')
            GROUP BY DATE_TRUNC('month', createdat)
        ) booking_rev ON months.month_start = booking_rev.month_start
        LEFT JOIN (
            SELECT DATE_TRUNC('month', createdat) AS month_start, SUM(refundamount) AS refund_total
            FROM refunds
            WHERE refundstatus = 'Refunded'
            AND createdat >= DATE_TRUNC('month', CURRENT_DATE - INTERVAL '11 months')
            GROUP BY DATE_TRUNC('month', createdat)
        ) refund_amt ON months.month_start = refund_amt.month_start
        ORDER BY months.month_start ASC
    """, nativeQuery = true)
    List<Object[]> getRevenueLast12Months();

    /**
     * Find booking by booking code
     */
    Optional<Booking> findByBookingCode(String bookingCode);

    /**
     * Find booking by ticket code
     */
    @Query("SELECT t.booking FROM Ticket t WHERE t.ticketCode = :ticketCode")
    Optional<Booking> findByTicketCode(@Param("ticketCode") String ticketCode);

    /**
     * Find booking by booking code with pessimistic lock (for IPN concurrency safety)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.bookingCode = :bookingCode")
    Optional<Booking> findByBookingCodeWithLock(@Param("bookingCode") String bookingCode);

    /**
     * Find booking by ID with pessimistic lock
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.bookingId = :bookingId")
    Optional<Booking> findByIdWithLock(@Param("bookingId") Integer bookingId);

    /**
     * Find all bookings for a customer
     */
    List<Booking> findByCustomer(User customer);

    /**
     * Find all bookings for a customer with pagination
     */
    Page<Booking> findByCustomer(User customer, Pageable pageable);

    /**
     * Find bookings by customer and status with pagination
     */
    Page<Booking> findByCustomerAndBookingStatus(User customer, String bookingStatus, Pageable pageable);

    /**
     * Find bookings by customer with status filter (multiple statuses) and future departure time
     */
    @Query("SELECT b FROM Booking b WHERE b.customer = :customer AND b.bookingStatus IN :statuses AND b.trip.departureTime > :departureTime")
    Page<Booking> findByCustomerAndBookingStatusInAndTripDepartureTimeAfter(
            @Param("customer") User customer, 
            @Param("statuses") List<String> statuses, 
            @Param("departureTime") LocalDateTime departureTime, 
            Pageable pageable);

    /**
     * Count all bookings for a customer
     */
    Long countByCustomer(User customer);

    /**
     * Count bookings by customer and status
     */
    Long countByCustomerAndBookingStatus(User customer, String bookingStatus);

    /**
     * Count bookings by customer with status filter (multiple statuses) and future departure time
     */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.customer = :customer AND b.bookingStatus IN :statuses AND b.trip.departureTime > :departureTime")
    Long countByCustomerAndBookingStatusInAndTripDepartureTimeAfter(
            @Param("customer") User customer, 
            @Param("statuses") List<String> statuses, 
            @Param("departureTime") LocalDateTime departureTime);

    /**
     * Find all bookings for a customer by customer ID
     */
    @Query("SELECT b FROM Booking b WHERE b.customer.userId = :customerId ORDER BY b.createdAt DESC")
    List<Booking> findByCustomerId(@Param("customerId") Integer customerId);

    /**
     * Find all bookings for a customer by customer ID and status
     */
    @Query("SELECT b FROM Booking b WHERE b.customer.userId = :customerId AND b.bookingStatus = :status ORDER BY b.createdAt DESC")
    List<Booking> findByCustomerIdAndBookingStatus(@Param("customerId") Integer customerId, @Param("status") String status);

    /**
     * Find all bookings for a trip
     */
    @Query("SELECT b FROM Booking b WHERE b.trip.tripId = :tripId")
    List<Booking> findByTripId(@Param("tripId") Integer tripId);

    /**
     * Find all bookings with status
     */
    List<Booking> findByBookingStatus(String bookingStatus);

    /**
     * Find guest booking by session ID
     */
    @Query("SELECT b FROM Booking b WHERE b.guestSessionId = :sessionId AND b.isGuestBooking = true")
    List<Booking> findGuestBookingsBySessionId(@Param("sessionId") String sessionId);

    /**
     * Find booking by phone number
     */
    @Query("SELECT b FROM Booking b WHERE b.customerPhone = :phone ORDER BY b.createdAt DESC")
    List<Booking> findByCustomerPhone(@Param("phone") String phone);

    /**
     * Find booking by email
     */
    @Query("SELECT b FROM Booking b WHERE b.customerEmail = :email ORDER BY b.createdAt DESC")
    List<Booking> findByCustomerEmail(@Param("email") String email);

    /**
     * Find expired bookings based on holdExpiry timestamp.
     * CRITICAL: Use holdExpiry (not createdAt or updatedAt) to determine expiration.
     * 
     * Logic:
     * - Booking created at 10:00 with holdExpiry = 10:15
     * - User creates payment at 10:12 → holdExpiry extended to 10:27
     * - At 10:27, if still HELD/PENDING → expire it
     * 
     * @param now Current timestamp
     * @return List of bookings that have exceeded their hold time
     */
    @Query("SELECT b FROM Booking b WHERE b.bookingStatus IN ('Held', 'Pending') AND b.holdExpiry < :now")
    List<Booking> findExpiredBookings(@Param("now") LocalDateTime now);

    /**
     * Bulk update expired bookings to EXPIRED status.
     * Uses holdExpiry for accurate timeout tracking.
     * 
     * @param now Current timestamp
     * @return Number of bookings updated
     */
    @Modifying
    @Query("UPDATE Booking b SET b.bookingStatus = 'Expired', b.updatedAt = :now WHERE b.bookingStatus IN ('Held', 'Pending') AND b.holdExpiry < :now")
    int updateExpiredBookingsStatus(@Param("now") LocalDateTime now);

    /**
     * Count bookings for a trip
     */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.trip.tripId = :tripId AND b.bookingStatus NOT IN ('Cancelled')")
    Long countActiveBookingsForTrip(@Param("tripId") Integer tripId);

    /**
     * Get the latest booking code sequence number for today
     */
    @Query(value = """
        SELECT MAX(CAST(SUBSTRING(bookingcode, 11) AS INTEGER))
        FROM bookings
        WHERE bookingcode LIKE CONCAT('BK', :datePrefix, '%')
    """, nativeQuery = true)
    Integer getLatestBookingSequence(@Param("datePrefix") String datePrefix);

    /**
     * Find all bookings with pagination and optional filtering
     */
    @Query("""
        SELECT DISTINCT b FROM Booking b 
        LEFT JOIN FETCH b.customer 
        LEFT JOIN FETCH b.trip t
        LEFT JOIN FETCH t.route
        LEFT JOIN FETCH t.vehicle
        LEFT JOIN FETCH t.driver
        WHERE (:status IS NULL OR b.bookingStatus = :status)
        AND (:search IS NULL OR :search = '' OR 
             LOWER(b.bookingCode) LIKE LOWER(CONCAT('%', :search, '%')) OR
             LOWER(b.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR
             LOWER(b.customerPhone) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    Page<Booking> findAllWithFilters(
        @Param("status") String status,
        @Param("search") String search,
        Pageable pageable
    );

    /**
     * Count query for pagination - required for DISTINCT with JOIN FETCH
     */
    @Query("""
        SELECT COUNT(DISTINCT b) FROM Booking b 
        WHERE (:status IS NULL OR b.bookingStatus = :status)
        AND (:search IS NULL OR :search = '' OR 
             LOWER(b.bookingCode) LIKE LOWER(CONCAT('%', :search, '%')) OR
             LOWER(b.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR
             LOWER(b.customerPhone) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    long countAllWithFilters(
        @Param("status") String status,
        @Param("search") String search
    );

    /**
     * Find bookings by IDs to preserve order and avoid JOIN FETCH in pagination
     */
    @Query("""
        SELECT DISTINCT b FROM Booking b 
        LEFT JOIN FETCH b.customer 
        LEFT JOIN FETCH b.trip t
        LEFT JOIN FETCH t.route
        LEFT JOIN FETCH t.vehicle
        LEFT JOIN FETCH t.driver
        WHERE b.bookingId IN :ids
    """)
    List<Booking> findByIdsWithDetails(@Param("ids") List<Integer> ids);

    /**
     * Find bookings by status and updated before a specific time
     * Used by payment scheduler to check pending/failed payments
     */
    @Query("SELECT b FROM Booking b WHERE b.bookingStatus = :status AND b.updatedAt < :threshold")
    List<Booking> findByBookingStatusAndUpdatedAtBefore(
        @Param("status") String status,
        @Param("threshold") LocalDateTime threshold
    );
}
