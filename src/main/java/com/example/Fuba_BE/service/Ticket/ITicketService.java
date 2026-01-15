package com.example.Fuba_BE.service.Ticket;

import com.example.Fuba_BE.dto.Ticket.TicketCheckInRequestDTO;
import com.example.Fuba_BE.dto.Ticket.TicketCheckInResponseDTO;
import com.example.Fuba_BE.dto.Ticket.TicketExportDTO;
import com.example.Fuba_BE.dto.Ticket.TicketScanResponseDTO;

public interface ITicketService {
    TicketScanResponseDTO getTicketDetailsByCode(String ticketCode);

    /**
     * Check-in a ticket via QR code scan
     * @param request Check-in request containing ticket code
     * @return Check-in response with updated ticket status
     */
    TicketCheckInResponseDTO checkInTicket(TicketCheckInRequestDTO request);

    /**
     * Get ticket data for PDF export (Real data from DB)
     * @param ticketId Ticket ID
     * @return DTO containing formatted data for PDF
     */
    TicketExportDTO getTicketExportData(Integer ticketId);
}