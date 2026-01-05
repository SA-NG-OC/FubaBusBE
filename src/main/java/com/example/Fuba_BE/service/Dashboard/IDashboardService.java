package com.example.Fuba_BE.service.Dashboard;

import com.example.Fuba_BE.dto.Dashboard.DashboardChartDTO;
import com.example.Fuba_BE.dto.Dashboard.DashboardStatsDTO;
import com.example.Fuba_BE.dto.Dashboard.DashboardTripDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface IDashboardService {
    // 1. Lấy thống kê tổng quan (Revenue, Tickets, Vehicles...)
    DashboardStatsDTO getDashboardStats();

    // 2. Lấy dữ liệu biểu đồ
    DashboardChartDTO getDashboardCharts(int year);

    // 3. Lấy danh sách chuyến đi hôm nay (có phân trang)
    Page<DashboardTripDTO> getTodayTrips(LocalDate date, Pageable pageable);
}
