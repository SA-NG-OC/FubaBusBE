package com.example.Fuba_BE.dto.Booking;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for rescheduling/changing a booking to a new trip.
 * Handles the logic of cancelling old booking and creating new one with refund/extra fee.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request payload for rescheduling a booking to a new trip")
public class RescheduleRequest {

    @NotNull(message = "Old booking ID is required")
    @Schema(description = "ID of the booking to reschedule", example = "1")
    private Integer oldBookingId;

    @NotNull(message = "New trip ID is required")
    @Schema(description = "ID of the new trip", example = "2")
    private Integer newTripId;

    @NotEmpty(message = "At least one new seat must be selected")
    @Schema(description = "List of new seat IDs to book on the new trip")
    private List<Integer> newSeatIds;

    @NotNull(message = "User ID is required")
    @Schema(description = "User ID requesting the reschedule", example = "123")
    private String userId;

    @Schema(description = "Reason for rescheduling", example = "Thay đổi lịch trình")
    private String reason;

    @Valid
    @Schema(description = "Updated passenger info (optional, will keep old info if not provided)")
    private List<BookingConfirmRequest.PassengerInfo> passengers;
}
