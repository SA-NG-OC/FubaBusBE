package com.example.Fuba_BE.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.Fuba_BE.domain.entity.Ticket;

import jakarta.persistence.LockModeType;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Integer> {

    /**
     * Count sold tickets in date range
     */
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.ticketStatus IN ('Confirmed', 'USED') AND t.createdAt BETWEEN :start AND :end")
    long countSoldTickets(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @EntityGraph(attributePaths = {
            "booking",
            "booking.trip",
            "booking.trip.route",
            "booking.trip.route.origin",
            "booking.trip.route.destination",
            "booking.trip.vehicle",
            "booking.trip.vehicle.vehicleType",
            "booking.trip.driver",
            "booking.trip.driver.user",
            "seat"
    })
    Optional<Ticket> findByTicketCode(String ticketCode);

    /**
     * Find ticket by code with pessimistic lock for check-in
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "booking",
            "booking.trip",
            "booking.trip.route",
            "booking.trip.route.origin",
            "booking.trip.route.destination",
            "seat"
    })
    @Query("SELECT t FROM Ticket t WHERE t.ticketCode = :ticketCode")
    Optional<Ticket> findByTicketCodeWithLock(@Param("ticketCode") String ticketCode);

    /**
     * Find ticket by ID with pessimistic lock
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Ticket t WHERE t.ticketId = :ticketId")
    Optional<Ticket> findByIdWithLock(@Param("ticketId") Integer ticketId);

    /**
     * Find all tickets for a booking with seat eagerly loaded
     */
    @Query("SELECT t FROM Ticket t LEFT JOIN FETCH t.seat WHERE t.booking.bookingId = :bookingId")
    List<Ticket> findByBookingId(@Param("bookingId") Integer bookingId);

    /**
     * Find all tickets for multiple bookings (batch fetch to avoid N+1)
     */
    @Query("SELECT t FROM Ticket t JOIN FETCH t.seat WHERE t.booking.bookingId IN :bookingIds")
    List<Ticket> findByBookingIds(@Param("bookingIds") List<Integer> bookingIds);

    /**
     * Find all tickets for a trip
     */
    @Query("SELECT t FROM Ticket t " +
            "JOIN t.booking b " +
            "WHERE b.trip.tripId = :tripId")
    List<Ticket> findByTripId(@Param("tripId") Integer tripId);

    /**
     * Find all active tickets for a trip with seat info using JOIN FETCH.
     * Includes Confirmed, USED, Unconfirmed tickets (excludes Cancelled).
     */
    @Query("SELECT DISTINCT t FROM Ticket t " +
            "JOIN FETCH t.booking b " +
            "JOIN FETCH t.seat s " +
            "WHERE b.trip.tripId = :tripId " +
            "AND t.ticketStatus NOT IN ('Cancelled') " +
            "AND b.bookingStatus NOT IN ('Cancelled', 'Expired')")
    List<Ticket> findActiveTicketsByTripIdWithDetails(@Param("tripId") Integer tripId);

    /**
     * Find tickets by seat IDs for a specific trip
     */
    @Query("SELECT t FROM Ticket t " +
            "JOIN t.booking b " +
            "WHERE b.trip.tripId = :tripId " +
            "AND t.seat.seatId IN :seatIds " +
            "AND t.ticketStatus NOT IN ('Cancelled')")
    List<Ticket> findActiveTicketsByTripAndSeats(@Param("tripId") Integer tripId,
            @Param("seatIds") List<Integer> seatIds);

    /**
     * Check if seat is already booked for a trip
     */
    @Query("SELECT COUNT(t) > 0 FROM Ticket t " +
            "JOIN t.booking b " +
            "WHERE b.trip.tripId = :tripId " +
            "AND t.seat.seatId = :seatId " +
            "AND t.ticketStatus NOT IN ('Cancelled')")
    boolean isSeatBookedForTrip(@Param("tripId") Integer tripId, @Param("seatId") Integer seatId);

    /**
     * Find all tickets by customer phone
     */
    @Query("SELECT t FROM Ticket t " +
            "JOIN t.booking b " +
            "WHERE b.customerPhone = :phone " +
            "ORDER BY t.createdAt DESC")
    List<Ticket> findByCustomerPhone(@Param("phone") String phone);

    /**
     * Get the latest ticket code sequence number for today
     */
    @Query(value = """
                SELECT MAX(CAST(SUBSTRING(ticketcode, 11) AS INTEGER))
                FROM tickets
                WHERE ticketcode LIKE CONCAT('TK', :datePrefix, '%')
            """, nativeQuery = true)
    Integer getLatestTicketSequence(@Param("datePrefix") String datePrefix);

    /**
     * Count tickets for a booking
     */
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.booking.bookingId = :bookingId")
    Long countByBookingId(@Param("bookingId") Integer bookingId);

    @Query("SELECT COUNT(tk) " +
            "FROM Ticket tk " +
            "JOIN tk.booking b " +
            "JOIN b.trip t " +
            "WHERE t.departureTime BETWEEN :start AND :end " +
            "AND tk.ticketStatus IN ('Confirmed', 'Used') " +
            "AND t.status != 'Cancelled'")
    Long countSoldTicketsBetween(@Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(t) FROM Ticket t " +
            "JOIN t.booking b " +
            "WHERE b.trip.tripId = :tripId " +
            "AND t.ticketStatus NOT IN ('Cancelled') " +
            "AND b.bookingStatus NOT IN ('Cancelled', 'Expired')")
    long countActiveTicketsByTripId(@Param("tripId") Integer tripId);

    /**
     * Find tickets by trip ID and status
     */
    @Query("SELECT t FROM Ticket t " +
            "JOIN t.booking b " +
            "WHERE b.trip.tripId = :tripId " +
            "AND t.ticketStatus = :status")
    List<Ticket> findByTripIdAndTicketStatus(@Param("tripId") Integer tripId,
            @Param("status") String status);
}