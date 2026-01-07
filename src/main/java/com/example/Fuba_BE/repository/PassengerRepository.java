package com.example.Fuba_BE.repository;

import com.example.Fuba_BE.domain.entity.Passenger;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PassengerRepository extends JpaRepository<Passenger, Integer> {
    @EntityGraph(attributePaths = {"pickupLocation", "dropoffLocation"})
    Optional<Passenger> findByTicket_TicketId(Integer ticketId);
}
