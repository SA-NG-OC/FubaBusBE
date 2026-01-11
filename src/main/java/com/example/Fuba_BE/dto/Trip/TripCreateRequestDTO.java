package com.example.Fuba_BE.dto.Trip;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class TripCreateRequestDTO {

    @NotNull(message = "Route is required")
    private Integer routeId;

    @NotNull(message = "Vehicle is required")
    private Integer vehicleId;

    @NotNull(message = "Driver ID is required")
    private Integer driverId;

    private Integer subDriverId;

    @NotNull(message = "Date is required")
    @FutureOrPresent(message = "Date must be today or in the future")
    private LocalDate date;

    @NotNull(message = "Departure time is required")
    private LocalTime departureTime;

    @NotNull(message = "Arrival time is required")
    private LocalTime arrivalTime;

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price must be greater than or equal to 0")
    private BigDecimal price;
}
