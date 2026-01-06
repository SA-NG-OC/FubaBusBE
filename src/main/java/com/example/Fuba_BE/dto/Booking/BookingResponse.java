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
 * Response DTO for a confirmed booking.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response payload for a confirmed booking")
public class BookingResponse {

    @Schema(description = "Booking ID", example = "1")
    private Integer bookingId;

    @Schema(description = "Unique booking code", example = "BK20260105001")
    private String bookingCode;

    @Schema(description = "Trip ID", example = "1")
    private Integer tripId;

    @Schema(description = "Trip information")
    private TripInfo tripInfo;

    @Schema(description = "Customer name", example = "Nguyễn Văn A")
    private String customerName;

    @Schema(description = "Customer phone", example = "0912345678")
    private String customerPhone;

    @Schema(description = "Customer email", example = "customer@example.com")
    private String customerEmail;

    @Schema(description = "Total amount for the booking")
    private BigDecimal totalAmount;

    @Schema(description = "Booking status", example = "Held")
    private String bookingStatus;

    @Schema(description = "Booking type", example = "Online")
    private String bookingType;

    @Schema(description = "List of tickets in this booking")
    private List<TicketInfo> tickets;

    @Schema(description = "Booking created timestamp")
    private LocalDateTime createdAt;

    /**
     * Nested DTO for trip information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Trip information summary")
    public static class TripInfo {
        @Schema(description = "Trip ID", example = "1")
        private Integer tripId;

        @Schema(description = "Route name", example = "Hà Nội - Hải Phòng")
        private String routeName;

        @Schema(description = "Departure time")
        private LocalDateTime departureTime;

        @Schema(description = "Arrival time")
        private LocalDateTime arrivalTime;

        @Schema(description = "Vehicle license plate", example = "30A-12345")
        private String vehiclePlate;

        @Schema(description = "Driver name", example = "Trần Văn B")
        private String driverName;
    }

    /**
     * Nested DTO for ticket information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Ticket information")
    public static class TicketInfo {
        @Schema(description = "Ticket ID", example = "1")
        private Integer ticketId;

        @Schema(description = "Unique ticket code", example = "TK20260105001")
        private String ticketCode;

        @Schema(description = "Seat ID", example = "101")
        private Integer seatId;

        @Schema(description = "Seat number", example = "A1")
        private String seatNumber;

        @Schema(description = "Floor number", example = "1")
        private Integer floorNumber;

        @Schema(description = "Ticket price")
        private BigDecimal price;

        @Schema(description = "Ticket status", example = "Đã xác nhận")
        private String ticketStatus;

        @Schema(description = "Passenger information")
        private PassengerInfo passenger;
    }

    /**
     * Nested DTO for passenger information in response
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Passenger information")
    public static class PassengerInfo {
        @Schema(description = "Passenger ID", example = "1")
        private Integer passengerId;

        @Schema(description = "Passenger full name", example = "Nguyễn Văn B")
        private String fullName;

        @Schema(description = "Phone number", example = "0987654321")
        private String phoneNumber;

        @Schema(description = "Email", example = "passenger@example.com")
        private String email;

        @Schema(description = "Pickup address")
        private String pickupAddress;

        @Schema(description = "Dropoff address")
        private String dropoffAddress;
    }
}
