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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/trips")
@Tag(name = "Seat Map Management", description = "APIs for managing seat maps")
public class SeatMapController {

    private final SeatMapService seatMapService;

    public SeatMapController(SeatMapService seatMapService) {
        this.seatMapService = seatMapService;
    }

    @PostMapping("/{tripId}/seat-map/migrate")
    @Operation(summary = "Migrate seat map", description = "Create or update seat map for a trip")
    public ResponseEntity<ApiResponse<SeatMapResponse>> migrateSeatMap(
            @Parameter(description = "Trip ID", required = true)
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
    @Operation(summary = "Get seat map", description = "Retrieve seat map for a specific trip")
    public ResponseEntity<ApiResponse<SeatMapResponse>> getSeats(
            @Parameter(description = "Trip ID", required = true)
            @PathVariable Integer tripId
    ) {
        SeatMapResponse seatMap = seatMapService.getSeatMap(tripId);
        return ResponseEntity.ok(ApiResponse.success("Seat map retrieved successfully", seatMap));
    }
}
