package com.example.Fuba_BE.dto.Dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DashboardStatsDTO {
    private StatItem revenue;         // Total Revenue
    private StatItem ticketsSold;     // Tickets Sold
    private StatItem activeVehicles;  // Active Vehicles
    private StatItem activeDrivers;   // Active Drivers

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class StatItem {
        private BigDecimal value;     // Giá trị hiện tại (VD: 12450000)
        private double growth;        // Tăng trưởng (VD: 12.5)
        private String label;         // VD: "+12.5%" hoặc "+2"
        private boolean isIncrease;   // true = xanh lá, false = đỏ
    }
}