package com.example.Fuba_BE.dto.Vehicle;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VehicleSelectionDTO {
    private Integer vehicleId;
    private String licensePlate; // Biển số xe sẽ là tên hiển thị
    private String vehicleTypeName; // Kèm thêm loại xe cho dễ chọn (VD: 29A-12345 - Xe Khách)
}
