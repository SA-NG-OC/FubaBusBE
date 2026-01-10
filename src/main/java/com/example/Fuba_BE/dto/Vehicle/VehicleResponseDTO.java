package com.example.Fuba_BE.dto.Vehicle;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VehicleResponseDTO {
    private Integer vehicleid;
    private String licenseplate;
    private String vehicletype;
    private Integer totalseats;
    private String status;
}
