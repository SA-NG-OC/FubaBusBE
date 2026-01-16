package com.example.Fuba_BE.dto.Driver;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DriverSelectionDTO {
    private Integer driverId;
    private String driverName; // Lấy từ User fullName
    private String driverLicense; // Kèm bằng lái để phân biệt nếu trùng tên
    private String avatarUrl;
}
