package com.example.Fuba_BE.dto.Location;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationResponseDTO {
    private Integer locationId;
    private String locationName;
    private String address;
    private String province;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalDateTime createdAt;
}
