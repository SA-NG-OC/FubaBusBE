package com.example.Fuba_BE.dto.Driver;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for driver statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverStatsDTO {
    private long total;
    private long active;
    private long onLeave;
    private long inactive;
}
