package com.example.Fuba_BE.dto.Ticket;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response DTO after ticket check-in")
public class TicketCheckInResponseDTO {

    @Schema(description = "Ticket code", example = "TK-20260112-001")
    private String ticketCode;

    @Schema(description = "Previous ticket status before check-in", example = "Confirmed")
    private String previousStatus;

    @Schema(description = "New ticket status after check-in", example = "Used")
    private String newStatus;

    @Schema(description = "Check-in timestamp")
    private LocalDateTime checkInTime;

    @Schema(description = "Check-in method used", example = "QR")
    private String checkInMethod;

    @Schema(description = "Passenger name", example = "Nguyen Van A")
    private String passengerName;

    @Schema(description = "Seat number", example = "A1")
    private String seatNumber;

    @Schema(description = "Route information", example = "Ho Chi Minh → Da Lat")
    private String routeName;

    @Schema(description = "Departure time")
    private LocalDateTime departureTime;

    @Schema(description = "Trip ID")
    private Integer tripId;

    @Schema(description = "Vehicle license plate", example = "51B-12345")
    private String licensePlate;

    @Schema(description = "Check-in success message")
    private String message;
}
