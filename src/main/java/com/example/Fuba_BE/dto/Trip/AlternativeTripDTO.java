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
public class AlternativeTripDTO {
    private Integer tripId;
    private String routeName;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private String vehicleInfo;
    private String vehicleTypeName;
    private String driverName;
    private Double price;
    private Integer totalSeats;
    private Integer availableSeats;
    private Integer bookedSeats;
    private String status;
}
