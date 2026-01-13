package com.example.Fuba_BE.dto.Trip;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class TripUpdateRequestDTO {
    @NotNull(message = "Vehicle is required")
    private Integer vehicleId;

    @NotNull(message = "Driver is required")
    private Integer driverId;

    private Integer subDriverId; // Optional

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price must be positive")
    private BigDecimal price;
}
