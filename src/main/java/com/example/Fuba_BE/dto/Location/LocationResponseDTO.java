package com.example.Fuba_BE.dto.Location;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LocationResponseDTO {
    private Integer locationId;
    private String locationName;
}
