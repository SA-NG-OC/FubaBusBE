package com.example.Fuba_BE.dto.AdminReport;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor // Cần constructor rỗng cho JPQL
public class RouteAnalyticsRes {
    private Integer routeId;
    private String routeName;
    private BigDecimal totalRevenue;
    private Long vehicleCount; // Số lượng xe hoạt động
    private Long driverCount;  // Số lượng tài xế hoạt động

    // Constructor dùng cho JPQL Expression
    public RouteAnalyticsRes(Integer routeId, String routeName, BigDecimal totalRevenue, Long vehicleCount, Long driverCount) {
        this.routeId = routeId;
        this.routeName = routeName;
        this.totalRevenue = totalRevenue;
        this.vehicleCount = vehicleCount;
        this.driverCount = driverCount;
    }
}
