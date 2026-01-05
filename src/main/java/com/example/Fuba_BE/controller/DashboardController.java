package com.example.Fuba_BE.controller;

import com.example.Fuba_BE.dto.Dashboard.DashboardChartDTO;
import com.example.Fuba_BE.dto.Dashboard.DashboardStatsDTO;
import com.example.Fuba_BE.dto.Dashboard.DashboardTripDTO;
import com.example.Fuba_BE.service.Dashboard.IDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.Year;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final IDashboardService dashboardService;

    // 1. API Thống kê tổng quan (4 thẻ bài)
    // GET /api/v1/dashboard/stats
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDTO> getStats() {
        return ResponseEntity.ok(dashboardService.getDashboardStats());
    }

    // 2. API Biểu đồ
    // GET /api/v1/dashboard/charts?year=2024
    @GetMapping("/charts")
    public ResponseEntity<DashboardChartDTO> getCharts(
            @RequestParam(required = false) Integer year
    ) {
        if (year == null) year = Year.now().getValue();
        return ResponseEntity.ok(dashboardService.getDashboardCharts(year));
    }

    // 3. API Danh sách chuyến đi (Table)
    // GET /api/v1/dashboard/todays-trips?date=2024-01-05&page=0&size=5
    @GetMapping("/todays-trips")
    public ResponseEntity<Page<DashboardTripDTO>> getTodayTrips(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        if (date == null) date = LocalDate.now();
        // Sắp xếp theo giờ khởi hành tăng dần
        Pageable pageable = PageRequest.of(page, size, Sort.by("departureTime").ascending());

        return ResponseEntity.ok(dashboardService.getTodayTrips(date, pageable));
    }
}
