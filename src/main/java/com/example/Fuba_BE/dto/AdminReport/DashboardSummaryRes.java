package com.example.Fuba_BE.dto.AdminReport;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardSummaryRes {
    private MetricData revenue;
    private MetricData costs;
    private MetricData netProfit;
    private MetricData occupancyRate;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MetricData {
        private BigDecimal value;
        private Double growthPercent; // % tăng trưởng so với tháng trước
    }
}
