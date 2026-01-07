package com.example.Fuba_BE.dto.Report;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class TicketSalesReportDTO {
    private long totalSeats;        // Tổng số ghế cung ứng (Capacity)
    private long ticketsSold;       // Số vé đã bán (Booked)
    private double occupancyRate;   // Tỉ lệ lấp đầy (%)
    private BigDecimal totalRevenue; // Doanh thu thực tế (Số vé * Giá vé)
}
