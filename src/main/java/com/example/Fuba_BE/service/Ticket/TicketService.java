package com.example.Fuba_BE.service.Ticket;

import com.example.Fuba_BE.domain.entity.Passenger;
import com.example.Fuba_BE.domain.entity.Ticket;
import com.example.Fuba_BE.dto.Ticket.TicketScanResponseDTO;
import com.example.Fuba_BE.exception.ResourceNotFoundException; // Custom Exception assumed
import com.example.Fuba_BE.mapper.TicketMapper;
import com.example.Fuba_BE.repository.PassengerRepository;
import com.example.Fuba_BE.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketService implements ITicketService {

    private final TicketRepository ticketRepository;
    private final PassengerRepository passengerRepository;
    private final TicketMapper ticketMapper;

    @Override
    @Transactional(readOnly = true)
    public TicketScanResponseDTO getTicketDetailsByCode(String ticketCode) {
        // 1. Fetch Ticket with deep fetching (using EntityGraph or JoinFetch in Repo is recommended to avoid N+1)
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with code: " + ticketCode));

        // 2. Fetch Passenger associated with this ticket (Optional)
        Passenger passenger = passengerRepository.findByTicket_TicketId(ticket.getTicketId())
                .orElse(null);

        // 3. Use Mapper to build response
        return ticketMapper.toScanResponse(ticket, passenger);
    }
}