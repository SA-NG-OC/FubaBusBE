package com.example.Fuba_BE.dto.Vehicle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for vehicle statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleStatsDTO {
    private long total;
    private long operational;
    private long maintenance;
    private long inactive;
}
