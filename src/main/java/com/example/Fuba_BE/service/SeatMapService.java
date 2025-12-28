package com.example.Fuba_BE.service;

import com.example.Fuba_BE.dto.seat.MigrateSeatMapRequest;
import com.example.Fuba_BE.dto.seat.SeatMapResponse;

public interface SeatMapService {
    SeatMapResponse migrateSeatMap(Integer tripId, MigrateSeatMapRequest request);

    SeatMapResponse getSeatMap(Integer tripId);
}
