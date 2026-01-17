package com.example.Fuba_BE.dto.Trip;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TripDetailedResponseDTO {
    private Integer tripId;
    private String routeName;       // VD: HCM -> Da Lat
    private String vehicleInfo;     // VD: 51A-12345 (Sleeper)
    private String driverName;
    private String subDriverName;
    private LocalDate date;         // VD: 2025-11-20
    private LocalTime departureTime; // VD: 06:00
    private LocalTime arrivalTime;
    private BigDecimal price;       // VD: 250000
    private String status;          // VD: Waiting
    private int totalSeats;      // Tổng ghế (Capacity)
    private int bookedSeats;     // Đã đặt (Sold/Booked)
    private int checkedInSeats;  // Đã lên xe (Checked-in)
    private String originName;      // Ví dụ: "Hồ Chí Minh"
    private String destinationName; // Ví dụ: "Đà Lạt"

    private String vehicleTypeName; // Ví dụ: "Giường nằm 34 phòng" (để hiển thị Badge)
    private String licensePlate;    // Ví dụ: "51B-123.45"
}
