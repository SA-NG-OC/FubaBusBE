package com.example.Fuba_BE.service.Ticket;

import com.example.Fuba_BE.domain.entity.Passenger;
import com.example.Fuba_BE.domain.entity.Ticket;
import com.example.Fuba_BE.dto.Ticket.TicketScanResponseDTO;

public interface ITicketService {
    TicketScanResponseDTO getTicketDetailsByCode(String ticketCode);
}