package com.example.Fuba_BE.service.Analytics;

import com.example.Fuba_BE.dto.AdminReport.ChartDataRes;
import com.example.Fuba_BE.dto.AdminReport.DashboardSummaryRes;
import com.example.Fuba_BE.dto.AdminReport.RouteAnalyticsRes;
import com.example.Fuba_BE.repository.RouteRepository;
import com.example.Fuba_BE.repository.TicketRepository;
import com.example.Fuba_BE.repository.TripCostRepository;
import com.example.Fuba_BE.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
    private final TripRepository tripRepository;     // Inject thêm
    private final TicketRepository ticketRepository; // Inject thêm
    private final RouteRepository routeRepository;

    @Override
    public DashboardSummaryRes getDashboardSummary(int month, int year) {
        // 1. Xác định khung thời gian (Giữ nguyên code cũ)
        YearMonth currentYM = YearMonth.of(year, month);
        LocalDateTime startCurr = currentYM.atDay(1).atStartOfDay();
        LocalDateTime endCurr = currentYM.atEndOfMonth().atTime(23, 59, 59);

        YearMonth prevYM = currentYM.minusMonths(1);
        LocalDateTime startPrev = prevYM.atDay(1).atStartOfDay();
        LocalDateTime endPrev = prevYM.atEndOfMonth().atTime(23, 59, 59);

        // 2. Lấy dữ liệu tài chính (Giữ nguyên)
        BigDecimal revCurr = tripCostRepository.sumRevenueBetween(startCurr, endCurr);
        BigDecimal costCurr = tripCostRepository.sumCostBetween(startCurr, endCurr);
        BigDecimal profitCurr = tripCostRepository.sumProfitBetween(startCurr, endCurr);

        // --- TÍNH OCCUPANCY RATE REAL (THÁNG HIỆN TẠI) ---
        BigDecimal occupancyCurr = calculateOccupancy(startCurr, endCurr);

        // 3. Lấy dữ liệu tháng trước (Growth)
        BigDecimal revPrev = tripCostRepository.sumRevenueBetween(startPrev, endPrev);
        BigDecimal costPrev = tripCostRepository.sumCostBetween(startPrev, endPrev);
        BigDecimal profitPrev = tripCostRepository.sumProfitBetween(startPrev, endPrev);

        // --- TÍNH OCCUPANCY RATE REAL (THÁNG TRƯỚC) ---
        BigDecimal occupancyPrev = calculateOccupancy(startPrev, endPrev);

        // 4. Build Response (Giữ nguyên)
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

    private BigDecimal calculateOccupancy(LocalDateTime start, LocalDateTime end) {
        Long totalCapacity = tripRepository.sumTotalCapacityBetween(start, end);
        Long totalSold = ticketRepository.countSoldTicketsBetween(start, end);

        // Null check và tránh chia cho 0
        if (totalCapacity == null || totalCapacity == 0 || totalSold == null) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(totalSold)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalCapacity), 2, RoundingMode.HALF_UP);
    }

    // Trong class AnalyticsService
    @Override
    public byte[] exportMonthlyReport(int month, int year) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // --- 1. CHUẨN BỊ STYLE ---
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle percentStyle = createPercentStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);

            // --- 2. TẠO SHEET 1: OVERVIEW (Tổng quan) ---
            Sheet sheetOverview = workbook.createSheet("Dashboard Overview");

            // Title
            Row titleRow = sheetOverview.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BÁO CÁO TÀI CHÍNH THÁNG " + month + "/" + year);
            titleCell.setCellStyle(titleStyle);
            sheetOverview.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

            // -- Phần A: KPI Summary --
            DashboardSummaryRes summary = getDashboardSummary(month, year);

            int rowIdx = 2; // Bắt đầu từ dòng 2
            Row kpiHeader = sheetOverview.createRow(rowIdx++);
            kpiHeader.createCell(0).setCellValue("Metric");
            kpiHeader.createCell(1).setCellValue("Value");
            kpiHeader.createCell(2).setCellValue("Growth (%)");

            // Apply style cho header
            for(int i=0; i<3; i++) kpiHeader.getCell(i).setCellStyle(headerStyle);

            // Data KPI
            createKpiRow(sheetOverview, rowIdx++, "Total Revenue", summary.getRevenue(), currencyStyle, percentStyle);
            createKpiRow(sheetOverview, rowIdx++, "Total Costs", summary.getCosts(), currencyStyle, percentStyle);
            createKpiRow(sheetOverview, rowIdx++, "Net Profit", summary.getNetProfit(), currencyStyle, percentStyle);
            createKpiRow(sheetOverview, rowIdx++, "Occupancy Rate", summary.getOccupancyRate(), null, percentStyle); // Rate ko cần format tiền

            // -- Phần B: Daily Revenue Trend --
            rowIdx += 2; // Cách ra 2 dòng
            Row chart1Header = sheetOverview.createRow(rowIdx++);
            Cell chart1Title = chart1Header.createCell(0);
            chart1Title.setCellValue("Doanh thu theo ngày (Daily Trend)");
            chart1Title.setCellStyle(headerStyle);
            sheetOverview.addMergedRegion(new CellRangeAddress(rowIdx-1, rowIdx-1, 0, 1));

            List<ChartDataRes> dailyData = getRevenueByDayOfWeek(month, year);
            for (ChartDataRes item : dailyData) {
                Row r = sheetOverview.createRow(rowIdx++);
                r.createCell(0).setCellValue("Ngày " + item.getLabel());
                Cell valCell = r.createCell(1);
                valCell.setCellValue(item.getValue().doubleValue());
                valCell.setCellStyle(currencyStyle);
            }

            // -- Phần C: Revenue By Shift --
            rowIdx += 2;
            Row chart2Header = sheetOverview.createRow(rowIdx++);
            Cell chart2Title = chart2Header.createCell(0);
            chart2Title.setCellValue("Doanh thu theo ca (Shifts)");
            chart2Title.setCellStyle(headerStyle);
            sheetOverview.addMergedRegion(new CellRangeAddress(rowIdx-1, rowIdx-1, 0, 1));

            List<ChartDataRes> shiftData = getRevenueByShift(month, year);
            for (ChartDataRes item : shiftData) {
                Row r = sheetOverview.createRow(rowIdx++);
                r.createCell(0).setCellValue(item.getLabel());
                Cell valCell = r.createCell(1);
                valCell.setCellValue(item.getValue().doubleValue());
                valCell.setCellStyle(currencyStyle);
            }

            // Auto size columns cho Sheet 1
            sheetOverview.autoSizeColumn(0);
            sheetOverview.autoSizeColumn(1);
            sheetOverview.autoSizeColumn(2);

            // --- 3. TẠO SHEET 2: ROUTE DETAILS (Chi tiết tuyến) ---
            Sheet sheetRoutes = workbook.createSheet("Route Analytics");

            // Header Row
            Row routeHeader = sheetRoutes.createRow(0);
            String[] headers = {"Route ID", "Route Name", "Total Revenue", "Vehicles", "Drivers"};
            for(int i=0; i<headers.length; i++) {
                Cell cell = routeHeader.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Lấy TẤT CẢ dữ liệu route (Pageable.unpaged())
            Page<RouteAnalyticsRes> allRoutes = getRouteAnalytics(month, year, Pageable.unpaged());

            int routeRowIdx = 1;
            for (RouteAnalyticsRes route : allRoutes) {
                Row r = sheetRoutes.createRow(routeRowIdx++);
                r.createCell(0).setCellValue(route.getRouteId());
                r.createCell(1).setCellValue(route.getRouteName());

                Cell revCell = r.createCell(2);
                revCell.setCellValue(route.getTotalRevenue().doubleValue());
                revCell.setCellStyle(currencyStyle);

                r.createCell(3).setCellValue(route.getVehicleCount());
                r.createCell(4).setCellValue(route.getDriverCount());
            }

            // Auto size columns cho Sheet 2
            for(int i=0; i<5; i++) sheetRoutes.autoSizeColumn(i);

            // --- 4. GHI RA OUTPUT ---
            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Error exporting excel report", e);
        }
    }

    // --- CÁC HÀM HELPER STYLE & ROW ---

    private void createKpiRow(Sheet sheet, int rowIndex, String title, DashboardSummaryRes.MetricData data, CellStyle valStyle, CellStyle percentStyle) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(title);

        // --- SỬA LỖI TẠI ĐÂY ---
        // Nếu giá trị là null (do chưa có data) thì gán mặc định là 0
        BigDecimal safeValue = (data.getValue() != null) ? data.getValue() : BigDecimal.ZERO;

        Cell valCell = row.createCell(1);
        valCell.setCellValue(safeValue.doubleValue());

        // Logic style giữ nguyên
        if (valStyle != null) valCell.setCellStyle(valStyle);
        else valCell.setCellValue(safeValue.doubleValue() + "%");

        // Xử lý Growth Percent an toàn luôn
        Double safeGrowth = (data.getGrowthPercent() != null) ? data.getGrowthPercent() : 0.0;

        Cell growthCell = row.createCell(2);
        growthCell.setCellValue(safeGrowth / 100.0);
        growthCell.setCellStyle(percentStyle);
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }

    private CellStyle createTitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        DataFormat format = wb.createDataFormat();
        // Format: #,##0 ₫
        style.setDataFormat(format.getFormat("#,##0 \"₫\""));
        return style;
    }

    private CellStyle createPercentStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        DataFormat format = wb.createDataFormat();
        style.setDataFormat(format.getFormat("0.00%"));
        // Tô màu đỏ nếu âm, xanh nếu dương (logic nâng cao, ở đây để mặc định)
        return style;
    }
}
