package com.example.Fuba_BE.service.Analytics;

import com.example.Fuba_BE.dto.AdminReport.ChartDataRes;
import com.example.Fuba_BE.dto.AdminReport.DashboardSummaryRes;
import com.example.Fuba_BE.dto.AdminReport.RouteAnalyticsRes;
import com.example.Fuba_BE.repository.RouteRepository;
import com.example.Fuba_BE.repository.TripCostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsService implements IAnalyticsService {

    private final TripCostRepository tripCostRepository;
    private final RouteRepository routeRepository;

    @Override
    public DashboardSummaryRes getDashboardSummary(int month, int year) {
        // 1. Xác định khung thời gian
        YearMonth currentYM = YearMonth.of(year, month);
        LocalDateTime startCurr = currentYM.atDay(1).atStartOfDay();
        LocalDateTime endCurr = currentYM.atEndOfMonth().atTime(23, 59, 59);

        YearMonth prevYM = currentYM.minusMonths(1);
        LocalDateTime startPrev = prevYM.atDay(1).atStartOfDay();
        LocalDateTime endPrev = prevYM.atEndOfMonth().atTime(23, 59, 59);

        // 2. Lấy dữ liệu tháng hiện tại
        BigDecimal revCurr = tripCostRepository.sumRevenueBetween(startCurr, endCurr);
        BigDecimal costCurr = tripCostRepository.sumCostBetween(startCurr, endCurr);
        BigDecimal profitCurr = tripCostRepository.sumProfitBetween(startCurr, endCurr);

        // TODO: Logic tính Occupancy Rate (Cần thêm Repo Ticket/Vehicle) - Tạm fix cứng hoặc tính riêng
        BigDecimal occupancyCurr = new BigDecimal("78.5");

        // 3. Lấy dữ liệu tháng trước để tính Growth
        BigDecimal revPrev = tripCostRepository.sumRevenueBetween(startPrev, endPrev);
        BigDecimal costPrev = tripCostRepository.sumCostBetween(startPrev, endPrev);
        BigDecimal profitPrev = tripCostRepository.sumProfitBetween(startPrev, endPrev);
        BigDecimal occupancyPrev = new BigDecimal("74.5");

        // 4. Build Response
        return DashboardSummaryRes.builder()
                .revenue(new DashboardSummaryRes.MetricData(revCurr, calculateGrowth(revCurr, revPrev)))
                .costs(new DashboardSummaryRes.MetricData(costCurr, calculateGrowth(costCurr, costPrev)))
                .netProfit(new DashboardSummaryRes.MetricData(profitCurr, calculateGrowth(profitCurr, profitPrev)))
                .occupancyRate(new DashboardSummaryRes.MetricData(occupancyCurr, calculateGrowth(occupancyCurr, occupancyPrev)))
                .build();
    }

    @Override
    public List<ChartDataRes> getRevenueByDayOfWeek(int month, int year) {
        YearMonth ym = YearMonth.of(year, month);
        return tripCostRepository.getRevenueByDayOfWeek(
                ym.atDay(1).atStartOfDay(),
                ym.atEndOfMonth().atTime(23, 59, 59)
        );
    }

    @Override
    public List<ChartDataRes> getRevenueByShift(int month, int year) {
        YearMonth ym = YearMonth.of(year, month);
        return tripCostRepository.getRevenueByShift(
                ym.atDay(1).atStartOfDay(),
                ym.atEndOfMonth().atTime(23, 59, 59)
        );
    }

    @Override
    public Page<RouteAnalyticsRes> getRouteAnalytics(int month, int year, Pageable pageable) {
        YearMonth ym = YearMonth.of(year, month);

        // Mặc định sort theo doanh thu giảm dần nếu user không request sort khác
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by("totalRevenue").descending());
        }

        return routeRepository.findRoutesWithAnalytics(
                ym.atDay(1).atStartOfDay(),
                ym.atEndOfMonth().atTime(23, 59, 59),
                pageable
        );
    }

    // Helper tính % tăng trưởng
    private Double calculateGrowth(BigDecimal current, BigDecimal previous) {
        if (current == null) current = BigDecimal.ZERO;
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) return 0.0;

        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(100))
                .doubleValue();
    }
}
