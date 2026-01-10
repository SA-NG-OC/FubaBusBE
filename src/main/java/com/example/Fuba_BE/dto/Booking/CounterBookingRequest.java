package com.example.Fuba_BE.dto.Booking;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for counter booking.
 * Counter bookings bypass seat locking and are created directly with Paid status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request payload for creating a counter booking (no seat locking)")
public class CounterBookingRequest {

    @NotNull(message = "Trip ID is required")
    @Schema(description = "ID of the trip", example = "1")
    private Integer tripId;

    @NotEmpty(message = "At least one seat must be selected")
    @Schema(description = "List of seat IDs to book")
    private List<Integer> seatIds;

    @NotBlank(message = "Customer name is required")
    @Size(min = 2, max = 100, message = "Customer name must be between 2 and 100 characters")
    @Schema(description = "Customer's full name", example = "Nguyễn Văn A")
    private String customerName;

    @NotBlank(message = "Customer phone is required")
    @Size(min = 10, max = 15, message = "Phone number must be between 10 and 15 digits")
    @Schema(description = "Customer's phone number", example = "0912345678")
    private String customerPhone;

    @Email(message = "Invalid email format")
    @Schema(description = "Customer's email (optional)", example = "customer@example.com")
    private String customerEmail;

    @NotBlank(message = "Staff user ID is required")
    @Schema(description = "User ID of the staff creating this booking", example = "123")
    private String staffUserId;

    @Valid
    @Schema(description = "List of passenger info for each seat (optional)")
    private List<PassengerInfo> passengers;

    @Schema(description = "Special notes for the booking")
    private String notes;

    /**
     * Nested DTO for passenger information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Passenger information for a specific seat")
    public static class PassengerInfo {
        
        @NotNull(message = "Seat ID is required")
        @Schema(description = "Seat ID for this passenger", example = "1")
        private Integer seatId;

        @NotBlank(message = "Passenger name is required")
        @Size(min = 2, max = 100, message = "Passenger name must be between 2 and 100 characters")
        @Schema(description = "Passenger's full name", example = "Trần Thị B")
        private String passengerName;

        @Schema(description = "Passenger's phone number", example = "0987654321")
        private String passengerPhone;

        @Schema(description = "Passenger's ID number (CMND/CCCD)", example = "012345678901")
        private String idNumber;

        @NotNull(message = "Pickup stop ID is required")
        @Schema(description = "Route stop ID for pickup", example = "1")
        private Integer pickupStopId;

        @NotNull(message = "Dropoff stop ID is required")
        @Schema(description = "Route stop ID for dropoff", example = "5")
        private Integer dropoffStopId;
    }
}
