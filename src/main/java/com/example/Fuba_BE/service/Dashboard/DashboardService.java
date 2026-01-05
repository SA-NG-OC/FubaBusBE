package com.example.Fuba_BE.service.Dashboard;

import com.example.Fuba_BE.dto.Dashboard.DashboardChartDTO;
import com.example.Fuba_BE.dto.Dashboard.DashboardStatsDTO;
import com.example.Fuba_BE.dto.Dashboard.DashboardTripDTO;
import com.example.Fuba_BE.domain.entity.Trip;
import com.example.Fuba_BE.mapper.DashboardMapper;
import com.example.Fuba_BE.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService implements IDashboardService {

    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final DashboardMapper dashboardMapper;

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsDTO getDashboardStats() {
        // ... (Giữ nguyên logic tính Stats của bạn, nó sẽ nhanh nếu ĐÃ CÓ INDEX ở Bước 1)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = YearMonth.now().atDay(1).atStartOfDay();
        LocalDateTime startOfLastMonth = YearMonth.now().minusMonths(1).atDay(1).atStartOfDay();
        LocalDateTime endOfLastMonth = YearMonth.now().minusMonths(1).atEndOfMonth().atTime(LocalTime.MAX);

        BigDecimal currentRevenue = bookingRepository.sumRevenueBetween(startOfMonth, now);
        BigDecimal lastMonthRevenue = bookingRepository.sumRevenueBetween(startOfLastMonth, endOfLastMonth);
        if (currentRevenue == null) currentRevenue = BigDecimal.ZERO;
        if (lastMonthRevenue == null) lastMonthRevenue = BigDecimal.ZERO;
        double revGrowth = calculateGrowth(currentRevenue, lastMonthRevenue);

        long currentTickets = ticketRepository.countSoldTickets(startOfMonth, now);
        long lastMonthTickets = ticketRepository.countSoldTickets(startOfLastMonth, endOfLastMonth);
        double ticketGrowth = calculateGrowth(BigDecimal.valueOf(currentTickets), BigDecimal.valueOf(lastMonthTickets));

        long activeVehicles = vehicleRepository.countByStatus("Operational");
        long activeDrivers = driverRepository.countActiveDrivers();

        return DashboardStatsDTO.builder()
                .revenue(createStatItem(currentRevenue, revGrowth, true))
                .ticketsSold(createStatItem(BigDecimal.valueOf(currentTickets), ticketGrowth, false))
                .activeVehicles(createStatItem(BigDecimal.valueOf(activeVehicles), 0, false))
                .activeDrivers(createStatItem(BigDecimal.valueOf(activeDrivers), 0, false))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardChartDTO getDashboardCharts(int year) {
        // ... (Giữ nguyên logic Charts, đảm bảo đã có Index cho bookings.created_at)
        List<Object[]> revenueData = bookingRepository.getRevenueTrends(year);
        List<DashboardChartDTO.ChartData> revenueChart = new ArrayList<>();
        for (Object[] row : revenueData) {
            revenueChart.add(new DashboardChartDTO.ChartData((String) row[0], (BigDecimal) row[1]));
        }

        List<Object[]> salesData = tripRepository.getWeeklyTicketSales();
        List<DashboardChartDTO.ChartData> salesChart = new ArrayList<>();
        for (Object[] row : salesData) {
            salesChart.add(new DashboardChartDTO.ChartData((String) row[0], BigDecimal.valueOf((Long) row[1])));
        }

        return DashboardChartDTO.builder()
                .revenueTrends(revenueChart)
                .weeklySales(salesChart)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DashboardTripDTO> getTodayTrips(LocalDate date, Pageable pageable) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        // Gọi hàm mới trả về Object[] (Trip + Count)
        Page<Object[]> page = tripRepository.findTripsWithBookingCount(startOfDay, endOfDay, pageable);

        return page.map(row -> {
            Trip trip = (Trip) row[0];
            Long bookedCount = (Long) row[1]; // Số ghế đã đặt lấy từ Subquery

            // Truyền bookedCount vào Mapper
            return dashboardMapper.toDashboardTripDTO(trip, bookedCount);
        });
    }

    // --- Helpers (Giữ nguyên) ---
    private double calculateGrowth(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) return 0.0;
        return current.subtract(previous)
                .divide(previous, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    private DashboardStatsDTO.StatItem createStatItem(BigDecimal value, double growth, boolean isCurrency) {
        return DashboardStatsDTO.StatItem.builder()
                .value(value)
                .growth(growth)
                .label((growth > 0 ? "+" : "") + growth + "%")
                .isIncrease(growth >= 0)
                .build();
    }
}