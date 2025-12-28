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
    private BigDecimal distance; // 308 km
    private String estimatedTime; // "6h" hoặc "2h 30m"
    private int totalStops; // 2 stops
    private List<String> stopNames; // ["Ho Chi Minh City", "Bien Hoa", "Di Linh", "Da Lat"]
    private String status; // "Active" / "Hoạt động"
}