package com.example.Fuba_BE.controller;

import com.example.Fuba_BE.dto.seat.MigrateSeatMapRequest;
import com.example.Fuba_BE.dto.seat.SeatMapResponse;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.service.SeatMapService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trips")
public class SeatMapController {

    private final SeatMapService seatMapService;

    public SeatMapController(SeatMapService seatMapService) {
        this.seatMapService = seatMapService;
    }

    // ✅ MIGRATION seats + seat_map
    @PostMapping("/{tripId}/seat-map/migrate")
    public ResponseEntity<ApiResponse<SeatMapResponse>> migrateSeatMap(
            @PathVariable Integer tripId,
            @RequestBody(required = false) MigrateSeatMapRequest request
    ) {
        if (request == null) {
            request = new MigrateSeatMapRequest();
        }

        SeatMapResponse seatMap = seatMapService.migrateSeatMap(tripId, request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Seat map migrated successfully",
                        seatMap
                )
        );
    }

    // ✅ API xem sơ đồ ghế (GET /api/trips/seats/{tripId})
    @GetMapping("/seats/{tripId}")
    public ResponseEntity<ApiResponse<SeatMapResponse>> getSeats(
            @PathVariable Integer tripId
    ) {
        SeatMapResponse seatMap = seatMapService.getSeatMap(tripId);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Get seat map successfully",
                        seatMap
                )
        );
    }
}
