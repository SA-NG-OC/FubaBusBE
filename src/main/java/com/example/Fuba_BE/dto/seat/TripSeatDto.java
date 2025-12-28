package com.example.Fuba_BE.dto.seat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripSeatDto {
    private Integer seatId;
    private String seatNumber;     // A1, A2, ... B1...
    private Integer floorNumber;   // 1=Lower, 2=Upper
    private String seatType;       // "Thường"
    private String status;         // "Trống" | "Đang giữ" | "Đã đặt"
    private LocalDateTime holdExpiry;
}
