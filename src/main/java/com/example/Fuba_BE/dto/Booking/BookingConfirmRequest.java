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
 * Request DTO for confirming a booking after seats have been locked.
 * All seats in the request must be locked by the same user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request payload for confirming a ticket booking")
public class BookingConfirmRequest {

    @NotNull(message = "Trip ID is required")
    @Schema(description = "ID of the trip", example = "1")
    private Integer tripId;

    @NotEmpty(message = "At least one seat must be selected")
    @Schema(description = "List of seat IDs to book")
    private List<Integer> seatIds;

    @NotBlank(message = "User ID is required")
    @Schema(description = "User ID who locked the seats", example = "user_123")
    private String userId;

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

    @Schema(description = "Whether this is a guest booking (not logged in)", example = "false")
    @Builder.Default
    private Boolean isGuestBooking = false;

    @Schema(description = "Session ID for guest bookings")
    private String guestSessionId;

    @Schema(description = "Payment ID for reference", example = "pay_abc123")
    private String paymentId;

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
        
        @NotNull(message = "Seat ID is required for passenger")
        @Schema(description = "Seat ID this passenger is assigned to", example = "101")
        private Integer seatId;

        @NotBlank(message = "Passenger name is required")
        @Schema(description = "Passenger's full name", example = "Nguyễn Văn B")
        private String fullName;

        @Schema(description = "Passenger's phone number", example = "0987654321")
        private String phoneNumber;

        @Email(message = "Invalid email format")
        @Schema(description = "Passenger's email", example = "passenger@example.com")
        private String email;

        @Schema(description = "Pickup stop ID")
        private Integer pickupStopId;

        @Schema(description = "Custom pickup address")
        private String pickupAddress;

        @Schema(description = "Dropoff stop ID")
        private Integer dropoffStopId;

        @Schema(description = "Custom dropoff address")
        private String dropoffAddress;

        @Schema(description = "Special notes for this passenger")
        private String specialNote;
    }
}
