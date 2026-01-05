package com.example.Fuba_BE.dto.Dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DashboardChartDTO {
    private List<ChartData> revenueTrends;   // Biểu đồ đường
    private List<ChartData> weeklySales;     // Biểu đồ cột

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ChartData {
        private String label;  // Trục X (VD: "Jan", "Feb" hoặc "Mon", "Tue")
        private BigDecimal value; // Trục Y (Doanh thu hoặc số vé)
    }
}