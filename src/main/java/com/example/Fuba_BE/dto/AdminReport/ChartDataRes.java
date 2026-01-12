package com.example.Fuba_BE.dto.AdminReport;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChartDataRes {
    private String label; // Tên thứ hoặc Khung giờ
    private BigDecimal value; // Doanh thu
}
