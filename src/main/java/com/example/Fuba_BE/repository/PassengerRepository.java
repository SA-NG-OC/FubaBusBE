package com.example.Fuba_BE.repository;

import com.example.Fuba_BE.domain.entity.Passenger;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PassengerRepository extends JpaRepository<Passenger, Integer> {
    /**
     * Find all passengers for a specific booking
     */
    @Query("SELECT p FROM Passenger p " +
           "JOIN p.ticket t " +
           "WHERE t.booking.bookingId = :bookingId")
    List<Passenger> findAllByBookingId(@Param("bookingId") Integer bookingId);

    /**
     * Find all passengers for a specific trip
     */
    @Query("SELECT p FROM Passenger p " +
           "JOIN p.ticket t " +
           "JOIN t.booking b " +
           "WHERE b.trip.tripId = :tripId")
    List<Passenger> findAllByTripId(@Param("tripId") Integer tripId);

    /**
     * Find passenger by phone number and booking
     */
    @Query("SELECT p FROM Passenger p " +
           "JOIN p.ticket t " +
           "WHERE t.booking.bookingId = :bookingId " +
           "AND p.phoneNumber = :phoneNumber")
    Optional<Passenger> findByBookingIdAndPhone(@Param("bookingId") Integer bookingId, 
                                                  @Param("phoneNumber") String phoneNumber);

    @EntityGraph(attributePaths = {"pickupLocation", "dropoffLocation"})
    Optional<Passenger> findByTicket_TicketId(Integer ticketId);
}
