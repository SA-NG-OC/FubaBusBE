package com.example.Fuba_BE.dto.Vehicle;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;

@Data
public class VehicleRequestDTO {

    @NotBlank(message = "License plate is required")
    @Size(max = 20, message = "License plate must be less than 20 characters")
    private String licensePlate;

    @NotNull(message = "Vehicle type ID is required")
    private Integer typeId;

    @Size(max = 50, message = "Insurance number must be less than 50 characters")
    private String insuranceNumber;

    @Future(message = "Insurance expiry date must be in the future")
    private LocalDate insuranceExpiry;

    private String status;
}
