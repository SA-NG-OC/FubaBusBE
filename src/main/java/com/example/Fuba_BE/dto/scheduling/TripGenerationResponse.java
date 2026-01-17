package com.example.Fuba_BE.dto.scheduling;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for trip generation response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripGenerationResponse {

    private Integer templateId;
    private String templateName;
    private LocalDate startDate;
    private LocalDate endDate;
    
    @Builder.Default
    private Integer totalDaysRequested = 0;
    
    @Builder.Default
    private Integer applicableDays = 0; // Days matching template's daysOfWeek
    
    @Builder.Default
    private Integer createdTrips = 0;
    
    @Builder.Default
    private Integer skippedCount = 0;
    
    @Builder.Default
    private List<String> skipReasons = new ArrayList<>();
    
    private GenerationBreakdown breakdown;
    
    private Boolean isDryRun;
    
    private String status; // Success, PartialSuccess, Failed
    
    private String message;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerationBreakdown {
        @Builder.Default
        private Integer outboundTrips = 0;
        
        @Builder.Default
        private Integer returnTrips = 0;
        
        @Builder.Default
        private Integer skippedAlreadyExists = 0;
        
        @Builder.Default
        private Integer skippedNoDriver = 0;
        
        @Builder.Default
        private Integer skippedNoVehicle = 0;
        
        @Builder.Default
        private Integer skippedDriverOverLimit = 0;
        
        @Builder.Default
        private Integer skippedOtherReasons = 0;
    }
}
