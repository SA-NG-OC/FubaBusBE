package com.example.Fuba_BE.dto.Ticket;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TicketExportDTO {
    // Header
    private String ticketCode;
    private String status;
    private String qrCodeBase64;

    // Timeline đón trả
    private String pickupTime;
    private String pickupLocation;
    private String dropoffTime;
    private String dropoffLocation;

    // Thông tin chuyến
    private String departureDate;
    private String vehicleType;
    private String licensePlate;
    private String driverName;

    // Hành khách
    private String passengerName;
    private String passengerPhone;
    private String passengerEmail;

    // Ghế & Tiền
    private String seatNumber;
    private String seatFloor; // Tầng trên/dưới
    private String totalPrice; // Đã format (Vd: 120.000 đ)
}
