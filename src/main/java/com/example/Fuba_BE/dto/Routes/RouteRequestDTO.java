package com.example.Fuba_BE.dto.Routes;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RouteRequestDTO {

    @NotBlank(message = "Route name must not be blank")
    private String routeName;

    @NotBlank(message = "Origin name must not be blank")
    private String originName;

    @NotBlank(message = "Destination name must not be blank")
    private String destinationName;

    @NotNull(message = "Distance must not be null")
    @Min(value = 1, message = "Distance must be greater than 0")
    private BigDecimal distance;

    @NotNull(message = "Estimated duration must not be null")
    @Min(value = 1, message = "Estimated duration must be greater than 0 minutes")
    private Integer estimatedDuration;

    // List of intermediate stop names
    private List<String> intermediateStopNames;
}
