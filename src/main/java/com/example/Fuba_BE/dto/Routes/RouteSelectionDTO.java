package com.example.Fuba_BE.dto.Routes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RouteSelectionDTO {
    private Integer routeId;
    private String routeName;
}
