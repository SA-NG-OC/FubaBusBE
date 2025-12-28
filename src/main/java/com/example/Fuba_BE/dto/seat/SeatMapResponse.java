package com.example.Fuba_BE.dto.seat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatMapResponse {
    private Integer tripId;
    private Integer vehicleId;
    private String vehicleTypeName;
    private Integer numberOfFloors;

    private List<FloorSeats> floors;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FloorSeats {
        private Integer floorNumber;      // 1/2
        private String floorLabel;        // "Lower Floor" / "Upper Floor"
        private List<TripSeatDto> seats;  // danh sách ghế
    }
}
