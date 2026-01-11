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
import java.util.stream.Collectors;

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
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.atEndOfMonth().atTime(23, 59, 59);

        List<Object[]> results = tripCostRepository.getRevenueByDayOfWeek(start, end);

        // Map từ Object[] sang DTO
        return results.stream()
                .map(row -> new ChartDataRes(
                        (String) row[0],           // label (1, 2, 3...)
                        (BigDecimal) row[1]        // revenue
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<ChartDataRes> getRevenueByShift(int month, int year) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.atEndOfMonth().atTime(23, 59, 59);

        List<Object[]> results = tripCostRepository.getRevenueByShift(start, end);

        // Map từ Object[] sang DTO
        return results.stream()
                .map(row -> new ChartDataRes(
                        (String) row[0],           // label (Morning, Afternoon...)
                        (BigDecimal) row[1]        // revenue
                ))
                .collect(Collectors.toList());
    }

    @Override
    public Page<RouteAnalyticsRes> getRouteAnalytics(int month, int year, Pageable pageable) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.atEndOfMonth().atTime(23, 59, 59);

        // Mặc định sort nếu chưa có (Native Query cần sort thủ công hoặc qua Pageable cẩn thận)
        // Lưu ý: Với Native Query, sort field phải khớp tên cột trong SELECT (ví dụ: totalRevenue)

        Page<Object[]> rawPage = routeRepository.findRoutesWithAnalytics(start, end, pageable);

        // Map từ Object[] sang DTO RouteAnalyticsRes
        return rawPage.map(row -> new RouteAnalyticsRes(
                (Integer) row[0],                // routeId
                (String) row[1],                 // routeName
                (BigDecimal) row[2],             // totalRevenue
                ((Number) row[3]).longValue(),   // vehicleCount (Postgres trả về BigInteger)
                ((Number) row[4]).longValue()    // driverCount
        ));
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
