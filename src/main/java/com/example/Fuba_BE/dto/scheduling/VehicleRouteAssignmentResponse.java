package com.example.Fuba_BE.dto.scheduling;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for vehicle-route assignment response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleRouteAssignmentResponse {

    private Integer assignmentId;
    private Integer vehicleId;
    private String vehicleLicensePlate;
    private String vehicleType;
    private Integer totalSeats;
    private Integer routeId;
    private String routeName;
    private String originName;
    private String destinationName;
    private Integer priority;
    private Boolean isActive;
    private LocalDate startDate;
    private LocalDate endDate;
    private String maintenanceSchedule;
    private LocalDate nextMaintenanceDate;
    private Boolean needsMaintenance;
    private String notes;
}
