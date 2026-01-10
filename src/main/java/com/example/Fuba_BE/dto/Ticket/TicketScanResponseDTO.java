package com.example.Fuba_BE.dto.Ticket;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class TicketScanResponseDTO {

    private TicketInfoDTO ticketInfo;
    private TripInfoDTO tripInfo;
    private PassengerInfoDTO passengerInfo;
    private SeatInfoDTO seatInfo;
    private LocationInfoDTO pickupInfo;
    private LocationInfoDTO dropoffInfo;

    @Data
    @Builder
    public static class TicketInfoDTO {
        private String ticketCode;
        private String bookingCode;
        private String status;
        private BigDecimal price;
    }

    @Data
    @Builder
    public static class TripInfoDTO {
        private String routeName;       // e.g., "Ho Chi Minh -> Da Lat"
        private LocalDate departureDate;
        private String timeRange;       // e.g., "06:00 - 12:30"
        private String duration;        // e.g., "(6h 30m)"
        private String vehicleType;     // e.g., "Limousine 24 seats"
        private String licensePlate;    // e.g., "VH-045"
        private String driverName;      // e.g., "Nguyen Van An"
    }

    @Data
    @Builder
    public static class PassengerInfoDTO {
        private String fullName;
        private String email;
        private String phoneNumber;
        private String cccd; // ID Card
    }

    @Data
    @Builder
    public static class SeatInfoDTO {
        private String seatNumber; // e.g., "A3"
        private String floor;      // e.g., "Upper deck"
        private String position;   // e.g., "Window"
    }

    @Data
    @Builder
    public static class LocationInfoDTO {
        private String locationName; // e.g., "Ben xe Mien Dong"
        private String address;      // e.g., "TP.HCM"
        private String time;         // e.g., "05:45"
    }
}