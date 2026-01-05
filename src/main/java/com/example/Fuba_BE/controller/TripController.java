package com.example.Fuba_BE.controller;

import java.time.LocalDate;
import java.util.List;

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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/trips")
@RequiredArgsConstructor
public class TripController {
    
    private final ITripService tripService;

    @GetMapping
        public ResponseEntity<ApiResponse<Page<TripDetailedResponseDTO>>> getTrips(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            
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
    public ResponseEntity<ApiResponse<List<LocalDate>>> getTripDates(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        List<LocalDate> dates = tripService.getDaysWithTrips(start, end);
        return ResponseEntity.ok(ApiResponse.success("Calendar dates retrieved successfully", dates));
    }

        @GetMapping("/by-date")
    public ResponseEntity<ApiResponse<List<TripDetailedResponseDTO>>> getTripsByDate(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<TripDetailedResponseDTO> trips = tripService.getTripsDetailsByDate(date);
        return ResponseEntity.ok(ApiResponse.success("Trips retrieved successfully", trips));
    }

        @PatchMapping("/{tripId}/status")
    public ResponseEntity<ApiResponse<Void>> updateTripStatus(
            @PathVariable Integer tripId,
            
            @Valid @RequestBody TripStatusUpdateRequestDTO request
    ) {
        tripService.updateTripStatus(tripId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Trip status updated successfully", null));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TripDetailedResponseDTO>> createTrip(
            @Valid @RequestBody TripCreateRequestDTO request
    ) {
        TripDetailedResponseDTO newTrip = tripService.createTrip(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Trip created successfully", newTrip));
    }
}
