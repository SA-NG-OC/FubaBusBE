package com.example.Fuba_BE.dto.Trip;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteTripRequestDTO {
    @NotNull(message = "Driver ID is required")
    private Integer driverId;

    private String completionNote;

    private Double actualDistanceKm;
}
