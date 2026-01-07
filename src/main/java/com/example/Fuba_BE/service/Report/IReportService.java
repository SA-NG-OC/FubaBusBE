package com.example.Fuba_BE.service.Report;

import com.example.Fuba_BE.dto.Report.TicketSalesReportDTO;
import com.example.Fuba_BE.dto.Report.TripOperationReportDTO;

import java.time.LocalDate;

public interface IReportService {
    // Báo cáo hoạt động chuyến
    TripOperationReportDTO getTripOperationReport(LocalDate fromDate, LocalDate toDate, Integer routeId);

    // Báo cáo vé và doanh thu
    TicketSalesReportDTO getTicketSalesReport(LocalDate fromDate, LocalDate toDate, Integer routeId);
}
