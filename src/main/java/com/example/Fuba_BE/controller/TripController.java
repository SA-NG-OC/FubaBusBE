package com.example.Fuba_BE.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.Fuba_BE.dto.Trip.TripCreateRequestDTO;
import com.example.Fuba_BE.dto.Trip.TripDetailedResponseDTO;
import com.example.Fuba_BE.dto.Trip.TripStatusUpdateRequestDTO;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.service.Trip.ITripService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/trips")
@RequiredArgsConstructor
@Tag(name = "Trip Management", description = "APIs for managing bus trips")
public class TripController {
    @Autowired
    private ITripService tripService;

    @GetMapping
    @Operation(
        summary = "Get all trips",
        description = "Retrieve trips with optional filters (status, date) and pagination support"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved trips",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid parameters"
        )
    })
    public ResponseEntity<ApiResponse<Page<TripDetailedResponseDTO>>> getTrips(
            @Parameter(description = "Filter by trip status")
            @RequestParam(required = false) String status,

            
            @Parameter(description = "Filter by date (format: yyyy-MM-dd)")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            
            @Parameter(hidden = true)
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = { "updatedAt", "createdAt" },
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        Page<TripDetailedResponseDTO> trips = tripService.getTripsByFilters(status, date, pageable);
        return ResponseEntity.ok(ApiResponse.success("Trips retrieved successfully", trips));
    }

    @GetMapping("/calendar-dates")
    @Operation(
        summary = "Get calendar dates with available trips",
        description = "Retrieve dates that have available trips within a date range"
    )
    public ResponseEntity<ApiResponse<List<LocalDate>>> getTripDates(
            @Parameter(description = "Start date (format: yyyy-MM-dd)", required = true)
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            
            @Parameter(description = "End date (format: yyyy-MM-dd)", required = true)
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        List<LocalDate> dates = tripService.getDaysWithTrips(start, end);
        return ResponseEntity.ok(ApiResponse.success("Calendar dates retrieved successfully", dates));
    }

    @GetMapping("/by-date")
    @Operation(
        summary = "Get trips by specific date",
        description = "Retrieve all trips scheduled for a specific date"
    )
    public ResponseEntity<ApiResponse<List<TripDetailedResponseDTO>>> getTripsByDate(
            @Parameter(description = "Date to filter trips (format: yyyy-MM-dd)", required = true)
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<TripDetailedResponseDTO> trips = tripService.getTripsDetailsByDate(date);
        return ResponseEntity.ok(ApiResponse.success("Trips retrieved successfully", trips));
    }

    @PatchMapping("/{tripId}/status")
    @Operation(
        summary = "Update trip status",
        description = "Update the status of a specific trip"
    )
    public ResponseEntity<ApiResponse<Void>> updateTripStatus(
            @Parameter(description = "Trip ID", required = true)
            @PathVariable Integer tripId,
            
            @Valid @RequestBody TripStatusUpdateRequestDTO request
    ) {
        tripService.updateTripStatus(tripId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Trip status updated successfully", null));
    }

    @PostMapping
    @Operation(
        summary = "Create new trip",
        description = "Create a new bus trip with route, vehicle, and driver information"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Trip created successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid input data"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Route, vehicle, or driver not found"
        )
    })
    public ResponseEntity<ApiResponse<TripDetailedResponseDTO>> createTrip(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Trip creation details",
                required = true
            )
            @Valid @RequestBody TripCreateRequestDTO request
    ) {
        TripDetailedResponseDTO newTrip = tripService.createTrip(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Trip created successfully", newTrip));
    }
}
