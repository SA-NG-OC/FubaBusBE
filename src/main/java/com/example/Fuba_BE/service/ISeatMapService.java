package com.example.Fuba_BE.service;

import com.example.Fuba_BE.dto.seat.MigrateSeatMapRequest;
import com.example.Fuba_BE.dto.seat.SeatMapResponse;

public interface ISeatMapService {
    SeatMapResponse migrateSeatMap(Integer tripId, MigrateSeatMapRequest request);

    SeatMapResponse getSeatMap(Integer tripId);
}
