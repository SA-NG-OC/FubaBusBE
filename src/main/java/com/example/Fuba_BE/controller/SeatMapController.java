package com.example.Fuba_BE.controller;

import com.example.Fuba_BE.dto.seat.MigrateSeatMapRequest;
import com.example.Fuba_BE.dto.seat.SeatMapResponse;
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

    // ✅ MIGRATION seats + seat_map v
    @PostMapping("/{tripId}/seat-map/migrate")
    public ResponseEntity<SeatMapResponse> migrateSeatMap(
            @PathVariable Integer tripId,
            @RequestBody(required = false) MigrateSeatMapRequest request
    ) {
        if (request == null) request = new MigrateSeatMapRequest();
        return ResponseEntity.ok(seatMapService.migrateSeatMap(tripId, request));
    }

    // ✅ API xem sơ đồ ghế (đúng task GET /trips/{id}/seats)
    @GetMapping("/seats/{tripId}")
    public ResponseEntity<SeatMapResponse> getSeats(@PathVariable Integer tripId) {
        return ResponseEntity.ok(seatMapService.getSeatMap(tripId));
    }
}
