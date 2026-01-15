package com.example.Fuba_BE.dto.scheduling;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for creating vehicle-route assignment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVehicleRouteAssignmentRequest {

    @NotNull(message = "Vehicle ID is required")
    private Integer vehicleId;

    @NotNull(message = "Route ID is required")
    private Integer routeId;

    @Min(value = 1, message = "Priority must be at least 1")
    @Max(value = 10, message = "Priority must not exceed 10")
    private Integer priority = 1;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate; // Optional

    @Pattern(regexp = "Weekly|Bi-weekly|Monthly|Quarterly", message = "Invalid maintenance schedule")
    private String maintenanceSchedule;

    private LocalDate lastMaintenanceDate;

    private LocalDate nextMaintenanceDate;

    private String notes;
}
