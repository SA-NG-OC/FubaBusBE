package com.example.Fuba_BE.controller;

import com.example.Fuba_BE.dto.Dashboard.DashboardChartDTO;
import com.example.Fuba_BE.dto.Dashboard.DashboardStatsDTO;
import com.example.Fuba_BE.dto.Dashboard.DashboardTripDTO;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.service.Dashboard.IDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.Year;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "APIs for dashboard statistics, charts, and monitoring")
public class DashboardController {

    private final IDashboardService dashboardService;

    // 1. API Thống kê tổng quan (4 thẻ bài)
    @GetMapping("/stats")
    @Operation(summary = "Get general statistics", description = "Retrieve summary statistics for the dashboard cards")
    public ResponseEntity<ApiResponse<DashboardStatsDTO>> getStats() {
        DashboardStatsDTO stats = dashboardService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success("Dashboard statistics retrieved successfully", stats));
    }

    // 2. API Biểu đồ
    @GetMapping("/charts")
    @Operation(summary = "Get chart data", description = "Retrieve data for revenue or trip charts by year")
    public ResponseEntity<ApiResponse<DashboardChartDTO>> getCharts() {
        DashboardChartDTO charts = dashboardService.getDashboardCharts();
        return ResponseEntity.ok(ApiResponse.success("Chart data retrieved successfully", charts));
    }

    // 3. API Danh sách chuyến đi trong ngày (Table)
    @GetMapping("/todays-trips")
    @Operation(summary = "Get trips for a specific date", description = "Retrieve paginated list of trips for monitoring")
    public ResponseEntity<ApiResponse<Page<DashboardTripDTO>>> getTodayTrips(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        // Xử lý giá trị mặc định
        LocalDate targetDate = (date == null) ? LocalDate.now() : date;

        // Tạo Pageable (Sắp xếp theo giờ khởi hành tăng dần)
        Pageable pageable = PageRequest.of(page, size, Sort.by("departureTime").ascending());

        Page<DashboardTripDTO> trips = dashboardService.getTodayTrips(targetDate, pageable);
        return ResponseEntity.ok(ApiResponse.success("Trips list retrieved successfully", trips));
    }
}