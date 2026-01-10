package com.example.Fuba_BE.controller;

import com.example.Fuba_BE.dto.Report.TicketSalesReportDTO;
import com.example.Fuba_BE.dto.Report.TripOperationReportDTO;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.service.Report.IReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    // Inject Interface thay vì Class cụ thể
    private final IReportService reportService;

    @GetMapping("/trip-operations")
    public ResponseEntity<ApiResponse<TripOperationReportDTO>> getTripOperations(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer routeId
    ) {
        TripOperationReportDTO report = reportService.getTripOperationReport(from, to, routeId);
        return ResponseEntity.ok(ApiResponse.success("Report generated successfully", report));
    }

    @GetMapping("/ticket-sales")
    public ResponseEntity<ApiResponse<TicketSalesReportDTO>> getTicketSales(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer routeId
    ) {
        TicketSalesReportDTO report = reportService.getTicketSalesReport(from, to, routeId);
        return ResponseEntity.ok(ApiResponse.success("Ticket sales report generated successfully", report));
    }
}
