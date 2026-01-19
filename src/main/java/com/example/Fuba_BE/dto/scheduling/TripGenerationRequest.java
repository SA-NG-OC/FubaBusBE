package com.example.Fuba_BE.dto.scheduling;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @Builder.Default
    private Boolean autoAssignDrivers = true;

    @Builder.Default
    private Boolean autoAssignVehicles = true;

    @Builder.Default
    private Boolean respectWorkingHourLimit = true;

    @Builder.Default
    private Boolean dryRun = false; // If true, only preview without creating trips

    @Builder.Default
    private Boolean skipExistingTrips = true; // Skip if trip already exists

    @Builder.Default
    private Boolean allowPartialGeneration = true; // Continue even if some trips fail
}
