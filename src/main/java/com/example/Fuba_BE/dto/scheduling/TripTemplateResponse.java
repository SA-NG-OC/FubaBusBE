package com.example.Fuba_BE.dto.scheduling;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for trip template response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripTemplateResponse {

    private Integer templateId;
    private String templateName;
    private Integer routeId;
    private String routeName;
    private String originName;
    private String destinationName;
    private Integer estimatedDuration; // minutes
    private LocalTime departureTime;
    private String daysOfWeek;
    private List<String> daysList;
    private BigDecimal basePrice;
    private Integer onlineBookingCutoff;
    private Integer minPassengers;
    private Integer maxPassengers;
    
    // Round-trip & interval
    private Boolean generateRoundTrip;
    private Integer intervalMinutes;
    private Integer tripsPerDay;
    private Integer totalTripsPerDay; // Calculated: tripsPerDay × (roundTrip ? 2 : 1)
    private Integer maxGenerationDays;
    private Boolean autoAssignDriver;
    private Boolean autoAssignVehicle;
    
    private Boolean autoCancelIfNotEnough;
    private Boolean isActive;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Boolean isCurrentlyEffective;
    private String notes;
}
