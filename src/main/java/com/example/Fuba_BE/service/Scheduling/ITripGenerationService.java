package com.example.Fuba_BE.service.Scheduling;

import com.example.Fuba_BE.dto.scheduling.TripGenerationRequest;
import com.example.Fuba_BE.dto.scheduling.TripGenerationResponse;

/**
 * Service interface for trip generation from templates
 */
public interface ITripGenerationService {

    /**
     * Generate trips from template with round-trip and interval support
     * 
     * @param request Generation parameters (template ID, date range, options)
     * @return Generation result with created trips count and skip reasons
     */
    TripGenerationResponse generateTripsFromTemplate(TripGenerationRequest request);

    /**
     * Preview trip generation without actually creating trips (dry-run)
     * 
     * @param request Generation parameters with dryRun=true
     * @return Preview result showing how many trips would be created
     */
    TripGenerationResponse previewTripGeneration(TripGenerationRequest request);

    /**
     * Validate if driver can take more trips without exceeding 10-hour limit
     * 
     * @param driverId Driver ID
     * @param date Work date
     * @param additionalHours Additional hours from new trip
     * @return true if driver can take the trip, false if would exceed limit
     */
    boolean validateDriverWorkingHours(Integer driverId, java.time.LocalDate date, double additionalHours);

    /**
     * Calculate total working hours for driver on specific date
     * 
     * @param driverId Driver ID
     * @param date Work date
     * @return Total hours worked on that date
     */
    double calculateDriverHoursOnDate(Integer driverId, java.time.LocalDate date);
}
