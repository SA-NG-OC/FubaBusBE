package com.example.Fuba_BE.dto.seat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripSeatDto {
    private Integer seatId;
    private String seatNumber;     // A1, A2, ... B1...
    private Integer floorNumber;   // 1=Lower, 2=Upper
    private String seatType;       // "Standard" | "VIP" | "Sleeper"
    private String status;         // "Available" | "Held" | "Booked"
    private LocalDateTime holdExpiry;
    private String lockedBy;       // User ID who locked the seat (null if available)
}
