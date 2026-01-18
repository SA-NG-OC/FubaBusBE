package com.example.Fuba_BE.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.Fuba_BE.domain.entity.Passenger;

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

       @EntityGraph(attributePaths = { "pickupLocation", "dropoffLocation" })
       Optional<Passenger> findByTicket_TicketId(Integer ticketId);

       /**
        * Find passenger by ticket ID
        */
       Optional<Passenger> findByTicketTicketId(Integer ticketId);

       /**
        * Find all passengers for multiple tickets (batch fetch to avoid N+1)
        */
       @EntityGraph(attributePaths = { "pickupLocation", "dropoffLocation" })
       @Query("SELECT p FROM Passenger p WHERE p.ticket.ticketId IN :ticketIds")
       List<Passenger> findByTicketIds(@Param("ticketIds") List<Integer> ticketIds);

       /**
        * Find all passengers for a trip with pickup/dropoff location info using
        * EntityGraph.
        */
       @EntityGraph(attributePaths = { "pickupLocation", "dropoffLocation", "checkedInBy", "ticket" })
       @Query("SELECT p FROM Passenger p " +
                     "JOIN p.ticket t " +
                     "JOIN t.booking b " +
                     "WHERE b.trip.tripId = :tripId " +
                     "AND t.ticketStatus NOT IN ('Cancelled') " +
                     "AND b.bookingStatus NOT IN ('Cancelled', 'Expired')")
       List<Passenger> findAllByTripIdWithDetails(@Param("tripId") Integer tripId);
}
