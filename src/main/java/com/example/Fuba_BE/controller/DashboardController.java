package com.example.Fuba_BE.controller;

import java.time.LocalDate;
import java.time.Year;

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

import com.example.Fuba_BE.dto.Dashboard.DashboardChartDTO;
import com.example.Fuba_BE.dto.Dashboard.DashboardStatsDTO;
import com.example.Fuba_BE.dto.Dashboard.DashboardTripDTO;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.service.Dashboard.IDashboardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final IDashboardService dashboardService;

    // 1. API Thống kê tổng quan (4 thẻ bài)
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsDTO>> getStats() {
        DashboardStatsDTO stats = dashboardService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success("Dashboard statistics retrieved successfully", stats));
    }

    // 2. API Biểu đồ
    @GetMapping("/charts")
    public ResponseEntity<ApiResponse<DashboardChartDTO>> getCharts(
            @RequestParam(required = false) Integer year
    ) {
        // Xử lý giá trị mặc định cho presentation layer
        int targetYear = (year == null) ? Year.now().getValue() : year;

        DashboardChartDTO charts = dashboardService.getDashboardCharts(targetYear);
        return ResponseEntity.ok(ApiResponse.success("Chart data retrieved successfully", charts));
    }

    // 3. API Danh sách chuyến đi trong ngày (Table)
    @GetMapping("/todays-trips")
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