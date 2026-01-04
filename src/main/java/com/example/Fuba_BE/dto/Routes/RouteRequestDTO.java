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

    @NotBlank(message = "Tên điểm đi không được để trống")
    private String originName; // Đổi từ ID sang Name

    @NotBlank(message = "Tên điểm đến không được để trống")
    private String destinationName; // Đổi từ ID sang Name

    @NotNull(message = "Khoảng cách không được để trống")
    @Min(value = 1, message = "Khoảng cách phải lớn hơn 0")
    private BigDecimal distance;

    @NotNull(message = "Thời gian ước tính không được để trống")
    @Min(value = 1, message = "Thời gian phải lớn hơn 0 phút")
    private Integer estimatedDuration;

    // Danh sách TÊN các trạm dừng ở giữa
    private List<String> intermediateStopNames; // Đổi từ List<Integer> sang List<String>
}