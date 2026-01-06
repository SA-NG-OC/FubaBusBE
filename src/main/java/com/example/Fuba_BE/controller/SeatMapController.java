package com.example.Fuba_BE.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Fuba_BE.dto.seat.MigrateSeatMapRequest;
import com.example.Fuba_BE.dto.seat.SeatMapResponse;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.service.SeatMapService;


@RestController
@RequestMapping("/trips")
public class SeatMapController {

    private final SeatMapService seatMapService;

    public SeatMapController(SeatMapService seatMapService) {
        this.seatMapService = seatMapService;
    }

        @PostMapping("/{tripId}/seat-map/migrate")
    public ResponseEntity<ApiResponse<SeatMapResponse>> migrateSeatMap(
            @PathVariable Integer tripId,
            
            @RequestBody(required = false) MigrateSeatMapRequest request
    ) {
        if (request == null) {
            request = new MigrateSeatMapRequest();
        }

        SeatMapResponse seatMap = seatMapService.migrateSeatMap(tripId, request);
        return ResponseEntity.ok(ApiResponse.success("Seat map migrated successfully", seatMap));
    }

    @GetMapping("/seats/{tripId}")
    public ResponseEntity<ApiResponse<SeatMapResponse>> getSeats(
            @PathVariable Integer tripId
    ) {
        SeatMapResponse seatMap = seatMapService.getSeatMap(tripId);
        return ResponseEntity.ok(ApiResponse.success("Seat map retrieved successfully", seatMap));
    }
}
