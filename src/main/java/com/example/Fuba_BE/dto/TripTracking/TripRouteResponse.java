package com.example.Fuba_BE.dto.TripTracking;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class TripRouteResponse {
    private Integer tripId;
    private String routeName;
    private PointInfo origin;      // Điểm đi
    private PointInfo destination; // Điểm đến

    // Class con để chứa thông tin toạ độ
    @Data
    @Builder
    public static class PointInfo {
        private String locationName;
        private String address;
        private BigDecimal latitude;
        private BigDecimal longitude;
    }
}