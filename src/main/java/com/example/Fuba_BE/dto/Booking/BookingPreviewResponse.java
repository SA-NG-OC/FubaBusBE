package com.example.Fuba_BE.dto.Booking;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for booking validation before confirm.
 * Used to show user the booking summary.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Booking preview/validation response")
public class BookingPreviewResponse {

    @Schema(description = "Whether all seats are valid for booking")
    private boolean valid;

    @Schema(description = "Validation message")
    private String message;

    @Schema(description = "Trip ID", example = "1")
    private Integer tripId;

    @Schema(description = "Trip details")
    private TripDetails tripDetails;

    @Schema(description = "List of seats to be booked")
    private List<SeatInfo> seats;

    @Schema(description = "Total amount to pay")
    private BigDecimal totalAmount;

    @Schema(description = "Lock expiry time - booking must complete before this")
    private LocalDateTime lockExpiry;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Trip details for preview")
    public static class TripDetails {
        private Integer tripId;
        private String routeName;
        private String departureLocation;
        private String arrivalLocation;
        private LocalDateTime departureTime;
        private LocalDateTime arrivalTime;
        private String vehicleType;
        private String vehiclePlate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Seat information for preview")
    public static class SeatInfo {
        private Integer seatId;
        private String seatNumber;
        private Integer floorNumber;
        private String seatType;
        private BigDecimal price;
        private String status;
        private String lockedBy;
        private LocalDateTime lockExpiry;
        private boolean validForBooking;
        private String validationMessage;
    }
}
