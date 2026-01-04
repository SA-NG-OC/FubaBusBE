package com.example.Fuba_BE.dto.Routes;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class RouteResponseDTO {
    private Integer routeId;
    private String routeName;
    private String originName;
    private String destinationName;
    private BigDecimal distance;       // 308 km
    private Integer estimatedDuration;  // số phút (ví dụ: 360)
    private int totalStops;
    private List<String> stopNames;
    private String status;
}
