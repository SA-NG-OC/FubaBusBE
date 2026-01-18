package com.example.Fuba_BE.dto.Vehicle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleTypeResponseDTO {
    private Integer typeId;
    private String typeName;
    private Integer totalSeats;
    private Integer numberOfFloors;
    private String description;
}
