package com.example.Fuba_BE.dto.scheduling;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for creating trip template
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTripTemplateRequest {

    @NotBlank(message = "Template name is required")
    @Size(max = 100, message = "Template name must not exceed 100 characters")
    private String templateName;

    @NotNull(message = "Route ID is required")
    private Integer routeId;

    @NotNull(message = "Departure time is required")
    private LocalTime departureTime;

    @NotBlank(message = "Days of week is required")
    @Pattern(regexp = "^(Daily|Weekdays|Weekends|((Mon|Tue|Wed|Thu|Fri|Sat|Sun)(,(Mon|Tue|Wed|Thu|Fri|Sat|Sun))*)$)", 
             message = "Invalid days of week format. Use: Daily, Weekdays, Weekends, or Mon,Tue,Wed")
    private String daysOfWeek;

    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.0", message = "Base price must be positive")
    private BigDecimal basePrice;

    @Min(value = 0, message = "Online booking cutoff must be non-negative")
    private Integer onlineBookingCutoff = 60;

    @Min(value = 0, message = "Min passengers must be non-negative")
    private Integer minPassengers = 1;

    @Min(value = 1, message = "Max passengers must be at least 1")
    private Integer maxPassengers = 40;

    // ========== ROUND-TRIP & INTERVAL CONFIGURATION ==========
    
    private Boolean generateRoundTrip = false;

    @Min(value = 0, message = "Interval minutes must be non-negative")
    private Integer intervalMinutes = 0;

    @Min(value = 1, message = "Trips per day must be at least 1")
    @Max(value = 20, message = "Trips per day must not exceed 20")
    private Integer tripsPerDay = 1;

    @Min(value = 1, message = "Max generation days must be at least 1")
    @Max(value = 365, message = "Max generation days must not exceed 365")
    private Integer maxGenerationDays = 31;

    private Boolean autoAssignDriver = true;

    private Boolean autoAssignVehicle = true;

    private Boolean autoCancelIfNotEnough = false;

    @NotNull(message = "Effective from date is required")
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo; // Optional

    private String notes;
}
