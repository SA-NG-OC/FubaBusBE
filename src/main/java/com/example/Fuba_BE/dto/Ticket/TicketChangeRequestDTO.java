package com.example.Fuba_BE.dto.Ticket;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketChangeRequestDTO {

    @NotNull(message = "Ticket ID is required")
    private Integer ticketId;

    @NotNull(message = "New trip ID is required")
    private Integer newTripId;

    @NotNull(message = "New seat ID is required")
    private Integer newSeatId;

    private String reason; // Reason for changing ticket
}
