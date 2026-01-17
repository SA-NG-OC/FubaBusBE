package com.example.Fuba_BE.dto.scheduling;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for trip generation request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripGenerationRequest {

    @NotNull(message = "Template ID is required")
    private Integer templateId;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private Boolean autoAssignDrivers = true;

    private Boolean autoAssignVehicles = true;

    private Boolean respectWorkingHourLimit = true;

    private Boolean dryRun = false; // If true, only preview without creating trips

    private Boolean skipExistingTrips = true; // Skip if trip already exists

    private Boolean allowPartialGeneration = true; // Continue even if some trips fail
}
