package com.example.Fuba_BE.dto.Dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DashboardTripDTO {
    private String tripIdDisplay; // Hiển thị dạng "TR-001"
    private String routeName;     // "HCM -> Da Lat"
    private String status;        // "Running", "Waiting"
    private String statusClass;   // CSS class cho status (VD: "text-green", "bg-yellow")
    private LocalTime departure;  // 06:00
    private String seatsInfo;     // "28/40"
}