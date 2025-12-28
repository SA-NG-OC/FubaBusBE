package com.example.Fuba_BE.dto.seat;

import lombok.Data;

@Data
public class MigrateSeatMapRequest {
    // nếu true: xoá seat_map cũ và tạo lại
    private boolean overwrite = false;
}
