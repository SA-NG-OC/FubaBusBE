package com.example.Fuba_BE.repository;

import com.example.Fuba_BE.domain.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Integer> {
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.ticketStatus IN ('Confirmed', 'Used') AND t.createdAt BETWEEN :start AND :end")
    long countSoldTickets(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
