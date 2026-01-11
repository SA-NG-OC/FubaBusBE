package com.example.Fuba_BE.controller;

import com.example.Fuba_BE.dto.AdminReport.ChartDataRes;
import com.example.Fuba_BE.dto.AdminReport.DashboardSummaryRes;
import com.example.Fuba_BE.dto.AdminReport.RouteAnalyticsRes;
import com.example.Fuba_BE.service.Analytics.IAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final IAnalyticsService analyticsService;

    // 1. Lấy thông tin tổng quan (KPI Cards)
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryRes> getSummary(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {

        if (month == null) month = LocalDate.now().getMonthValue();
        if (year == null) year = LocalDate.now().getYear();

        return ResponseEntity.ok(analyticsService.getDashboardSummary(month, year));
    }

    // 2. Biểu đồ Doanh thu theo Thứ
    @GetMapping("/revenue-weekly")
    public ResponseEntity<List<ChartDataRes>> getWeeklyRevenue(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {

        if (month == null) month = LocalDate.now().getMonthValue();
        if (year == null) year = LocalDate.now().getYear();

        return ResponseEntity.ok(analyticsService.getRevenueByDayOfWeek(month, year));
    }

    // 3. Biểu đồ Khung giờ
    @GetMapping("/revenue-shifts")
    public ResponseEntity<List<ChartDataRes>> getShiftRevenue(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {

        if (month == null) month = LocalDate.now().getMonthValue();
        if (year == null) year = LocalDate.now().getYear();

        return ResponseEntity.ok(analyticsService.getRevenueByShift(month, year));
    }

    // 4. Danh sách Top Routes (Có phân trang)
    @GetMapping("/top-routes")
    public ResponseEntity<Page<RouteAnalyticsRes>> getTopRoutes(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (month == null) month = LocalDate.now().getMonthValue();
        if (year == null) year = LocalDate.now().getYear();

        // Lưu ý: Sort được xử lý dynamic trong query JPQL dựa trên alias (totalRevenue)
        Pageable pageable = PageRequest.of(page, size, Sort.by("totalRevenue").descending());

        return ResponseEntity.ok(analyticsService.getRouteAnalytics(month, year, pageable));
    }
}
