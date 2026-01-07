package com.example.Fuba_BE.dto.Report;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class TripOperationReportDTO {
    private long totalTrips;              // Tổng số chuyến trong khoảng thời gian
    private long totalCompleted;          // Số chuyến hoàn thành
    private long totalCancelled;          // Số chuyến hủy
    private long totalDelayed;            // Số chuyến bị hoãn/sự cố
    private long totalRunning;            // Số chuyến đang chạy
    private long totalWaiting;

    // Thống kê doanh thu cơ bản (Tổng giá các chuyến)
    private BigDecimal estimatedRevenue;
}
