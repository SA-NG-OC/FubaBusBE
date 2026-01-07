package com.example.Fuba_BE.dto.TripTracking;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class LocationUpdateReq {
    private Integer tripId;
    private Integer driverId; // ID của User hoặc Driver
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal speed;
    private String direction; // Ví dụ: "NW", "120deg"
    private String trafficStatus; // Optional
}
