package com.example.Fuba_BE.repository;

import com.example.Fuba_BE.domain.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Integer> {

    /**
     * Count sold tickets in date range
     */
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.ticketStatus IN ('Confirmed', 'USED') AND t.createdAt BETWEEN :start AND :end")
    long countSoldTickets(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Find ticket by ticket code
     */
    Optional<Ticket> findByTicketCode(String ticketCode);

    /**
     * Find ticket by ID with pessimistic lock
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Ticket t WHERE t.ticketId = :ticketId")
    Optional<Ticket> findByIdWithLock(@Param("ticketId") Integer ticketId);

    /**
     * Find all tickets for a booking
     */
    @Query("SELECT t FROM Ticket t WHERE t.booking.bookingId = :bookingId")
    List<Ticket> findByBookingId(@Param("bookingId") Integer bookingId);

    /**
     * Find all tickets for a trip
     */
    @Query("SELECT t FROM Ticket t " +
           "JOIN t.booking b " +
           "WHERE b.trip.tripId = :tripId")
    List<Ticket> findByTripId(@Param("tripId") Integer tripId);

    /**
     * Find tickets by seat IDs for a specific trip
     */
    @Query("SELECT t FROM Ticket t " +
           "JOIN t.booking b " +
           "WHERE b.trip.tripId = :tripId " +
           "AND t.seat.seatId IN :seatIds " +
           "AND t.ticketStatus NOT IN ('CANCELLED')")
    List<Ticket> findActiveTicketsByTripAndSeats(@Param("tripId") Integer tripId, 
                                                   @Param("seatIds") List<Integer> seatIds);

    /**
     * Check if seat is already booked for a trip
     */
    @Query("SELECT COUNT(t) > 0 FROM Ticket t " +
           "JOIN t.booking b " +
           "WHERE b.trip.tripId = :tripId " +
           "AND t.seat.seatId = :seatId " +
           "AND t.ticketStatus NOT IN ('CANCELLED')")
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
}
