package com.example.Fuba_BE.dto.Routes;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RouteStopResponseDTO {
    private Integer routeStopId;
    private String routeStopName;
}
