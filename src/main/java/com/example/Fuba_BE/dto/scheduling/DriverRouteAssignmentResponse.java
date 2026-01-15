package com.example.Fuba_BE.dto.scheduling;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for driver-route assignment response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverRouteAssignmentResponse {

    private Integer assignmentId;
    private Integer driverId;
    private String driverName;
    private String driverLicense;
    private Integer routeId;
    private String routeName;
    private String originName;
    private String destinationName;
    private String preferredRole;
    private Integer priority;
    private Boolean isActive;
    private LocalDate startDate;
    private LocalDate endDate;
    private String notes;
}
