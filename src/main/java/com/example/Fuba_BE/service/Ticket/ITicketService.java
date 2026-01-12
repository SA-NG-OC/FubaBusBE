package com.example.Fuba_BE.service.Ticket;

import com.example.Fuba_BE.dto.Ticket.TicketCheckInRequestDTO;
import com.example.Fuba_BE.dto.Ticket.TicketCheckInResponseDTO;
import com.example.Fuba_BE.dto.Ticket.TicketScanResponseDTO;

public interface ITicketService {
    TicketScanResponseDTO getTicketDetailsByCode(String ticketCode);
    
    /**
     * Check-in a ticket via QR code scan
     * @param request Check-in request containing ticket code
     * @return Check-in response with updated ticket status
     */
    TicketCheckInResponseDTO checkInTicket(TicketCheckInRequestDTO request);
}