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
    private BigDecimal price;       // VD: 250000
    private String status;          // VD: Waiting
}
