package com.example.Fuba_BE.dto.Driver;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for driver details with user info and active routes
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverResponseDTO {

    private Integer driverId;
    private String driverLicense;
    private LocalDate licenseExpiry;
    private LocalDate dateOfBirth;
    private BigDecimal salary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // User information
    private Integer userId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String address;
    private String avatar;
    private String status; // Active, On Leave, etc.

    // Active route assignments
    private List<ActiveRouteDTO> activeRoutes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActiveRouteDTO {
        private Integer assignmentId;
        private Integer routeId;
        private String routeName;
        private String origin;
        private String destination;
        private String preferredRole; // Main, Backup
        private Integer priority;
        private LocalDate startDate;
        private LocalDate endDate;
    }
}
