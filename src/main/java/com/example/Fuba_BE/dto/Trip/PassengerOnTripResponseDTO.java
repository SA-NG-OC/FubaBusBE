package com.example.Fuba_BE.dto.Trip;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PassengerOnTripResponseDTO {

    private SeatInfoDTO seat;
    private TicketInfoDTO ticket;
    private PassengerInfoDTO passenger;
    private CheckinInfoDTO checkin;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeatInfoDTO {
        private Integer seatId;
        private String seatNumber;
        private Integer floorNumber;
        private String seatType;
        private String status; // Available, Booked, Held
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketInfoDTO {
        private Integer ticketId;
        private String ticketCode;
        private String bookingCode;
        private String ticketStatus; // Unconfirmed, Confirmed, Used, Cancelled, Refunded
        private BigDecimal price;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PassengerInfoDTO {
        private Integer passengerId;
        private String fullName;
        private String phoneNumber;
        private String email;
        private String pickupLocationName;
        private String pickupAddress;
        private String dropoffLocationName;
        private String dropoffAddress;
        private String specialNote;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckinInfoDTO {
        private String checkinStatus; // NotBoarded, Boarded, Completed
        private LocalDateTime checkinTime;
        private LocalDateTime checkoutTime;
        private String checkinMethod; // QR, Manual
        private String checkedInByName;
    }
}
