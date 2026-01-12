package com.example.Fuba_BE.dto.Ticket;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request DTO for ticket check-in via QR code")
public class TicketCheckInRequestDTO {

    @NotBlank(message = "Ticket code is required")
    @Schema(description = "Ticket code from QR scan", example = "TK-20260112-001")
    private String ticketCode;

    @Schema(description = "Trip ID that driver/staff is currently operating (for validation)", example = "123")
    private Integer tripId;

    @Schema(description = "Vehicle ID for additional validation", example = "45")
    private Integer vehicleId;

    @Schema(description = "Check-in method", example = "QR", allowableValues = {"QR", "Manual"})
    @Builder.Default
    private String checkInMethod = "QR";

    @Schema(description = "Optional note for check-in", example = "Passenger arrived early")
    private String note;
}
