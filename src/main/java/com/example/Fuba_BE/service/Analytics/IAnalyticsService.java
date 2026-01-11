package com.example.Fuba_BE.service.Analytics;

import com.example.Fuba_BE.dto.AdminReport.ChartDataRes;
import com.example.Fuba_BE.dto.AdminReport.DashboardSummaryRes;
import com.example.Fuba_BE.dto.AdminReport.RouteAnalyticsRes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IAnalyticsService {
    DashboardSummaryRes getDashboardSummary(int month, int year);
    List<ChartDataRes> getRevenueByDayOfWeek(int month, int year);
    List<ChartDataRes> getRevenueByShift(int month, int year);
    Page<RouteAnalyticsRes> getRouteAnalytics(int month, int year, Pageable pageable);
    byte[] exportMonthlyReport(int month, int year);
}
