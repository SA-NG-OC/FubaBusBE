package com.example.Fuba_BE.dto.Routes;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RouteRequestDTO {

    @NotBlank(message = "Tên tuyến không được để trống")
    private String routeName;

    @NotNull(message = "Điểm đi không được để trống")
    private Integer originId; // ID của Location (Start Point)

    @NotNull(message = "Điểm đến không được để trống")
    private Integer destinationId; // ID của Location (End Point)

    @NotNull(message = "Khoảng cách không được để trống")
    @Min(value = 1, message = "Khoảng cách phải lớn hơn 0")
    private BigDecimal distance;

    @NotNull(message = "Thời gian ước tính không được để trống")
    @Min(value = 1, message = "Thời gian phải lớn hơn 0 phút")
    private Integer estimatedDuration; // Nhận vào số phút (Ví dụ: 6h -> 360)

    // Danh sách ID các trạm dừng ở giữa (như Bien Hoa, Di Linh)
    private List<Integer> intermediateStopIds;
}