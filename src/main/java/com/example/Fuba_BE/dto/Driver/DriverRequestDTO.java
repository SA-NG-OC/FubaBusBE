package com.example.Fuba_BE.dto.Driver;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating/updating drivers
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverRequestDTO {

    @NotNull(message = "User ID is required")
    private Integer userId;

    @NotBlank(message = "Driver license is required")
    private String driverLicense;

    @NotNull(message = "License expiry date is required")
    private LocalDate licenseExpiry;

    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;

    private BigDecimal salary;
}
