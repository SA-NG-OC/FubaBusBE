package com.example.Fuba_BE.controller;

import com.example.Fuba_BE.dto.AdminReport.ChartDataRes;
import com.example.Fuba_BE.dto.AdminReport.DashboardSummaryRes;
import com.example.Fuba_BE.dto.AdminReport.RouteAnalyticsRes;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.service.Analytics.IAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AnalyticsController {

    private final IAnalyticsService analyticsService;

    // 1. KPI Summary
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryRes>> getSummary(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {

        if (month == null) month = LocalDate.now().getMonthValue();
        if (year == null) year = LocalDate.now().getYear();

        DashboardSummaryRes summary =
                analyticsService.getDashboardSummary(month, year);

        return ResponseEntity.ok(
                ApiResponse.success("Dashboard summary retrieved successfully", summary)
        );
    }

    // 2. Revenue by Day of Week
    @GetMapping("/revenue-weekly")
    public ResponseEntity<ApiResponse<List<ChartDataRes>>> getWeeklyRevenue(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {

        if (month == null) month = LocalDate.now().getMonthValue();
        if (year == null) year = LocalDate.now().getYear();

        List<ChartDataRes> data =
                analyticsService.getRevenueByDayOfWeek(month, year);

        return ResponseEntity.ok(
                ApiResponse.success("Weekly revenue data retrieved successfully", data)
        );
    }

    // 3. Revenue by Shift
    @GetMapping("/revenue-shifts")
    public ResponseEntity<ApiResponse<List<ChartDataRes>>> getShiftRevenue(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {

        if (month == null) month = LocalDate.now().getMonthValue();
        if (year == null) year = LocalDate.now().getYear();

        List<ChartDataRes> data =
                analyticsService.getRevenueByShift(month, year);

        return ResponseEntity.ok(
                ApiResponse.success("Shift revenue data retrieved successfully", data)
        );
    }

    // 4. Top Routes (Pagination)
    @GetMapping("/top-routes")
    public ResponseEntity<ApiResponse<Page<RouteAnalyticsRes>>> getTopRoutes(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (month == null) month = LocalDate.now().getMonthValue();
        if (year == null) year = LocalDate.now().getYear();

        Pageable pageable = PageRequest.of(page, size);

        Page<RouteAnalyticsRes> routes =
                analyticsService.getRouteAnalytics(month, year, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Top routes retrieved successfully", routes)
        );
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportReport(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {

        if (month == null) month = LocalDate.now().getMonthValue();
        if (year == null) year = LocalDate.now().getYear();

        byte[] excelContent = analyticsService.exportMonthlyReport(month, year);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report_" + month + "_" + year + ".xlsx")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .body(excelContent);
    }
}
