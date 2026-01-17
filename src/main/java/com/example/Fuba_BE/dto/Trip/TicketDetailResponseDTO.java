package com.example.Fuba_BE.dto.Trip;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketDetailResponseDTO {
    private Integer ticketId;
    private String ticketCode;
    private String status;

    // Passenger info
    private Integer passengerId;
    private String passengerName;
    private String phoneNumber;
    private String email;
    private String idCard;

    // Trip info
    private Integer tripId;
    private String routeName;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private String originName;
    private String destinationName;

    // Seat info
    private Integer seatId;
    private String seatNumber;
    private String seatType;
    private String seatStatus;

    // Booking info
    private Integer bookingId;
    private LocalDateTime bookingDate;
    private Double ticketPrice;
    private String paymentStatus;
    private String paymentMethod;

    // Vehicle info
    private String vehiclePlateNumber;
    private String vehicleType;
}
