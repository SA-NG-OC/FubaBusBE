package com.example.Fuba_BE.dto.scheduling;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating driver-route assignment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDriverRouteAssignmentRequest {

    @NotNull(message = "Driver ID is required")
    private Integer driverId;

    @NotNull(message = "Route ID is required")
    private Integer routeId;

    @NotBlank(message = "Preferred role is required")
    @Pattern(regexp = "Main|SubDriver", message = "Role must be Main or SubDriver")
    private String preferredRole;

    @Min(value = 1, message = "Priority must be at least 1")
    @Max(value = 10, message = "Priority must not exceed 10")
    @Builder.Default
    private Integer priority = 1;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate; // Optional - can be null for indefinite

    private String notes;
}
