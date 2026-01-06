package com.example.Fuba_BE.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.Fuba_BE.domain.entity.Trip;
import com.example.Fuba_BE.dto.Trip.TripCreateRequestDTO;
import com.example.Fuba_BE.dto.Trip.TripDetailedResponseDTO;
import com.example.Fuba_BE.dto.Trip.TripStatusUpdateRequestDTO;
import com.example.Fuba_BE.mapper.TripMapper; // Import Mapper
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.service.Trip.ITripService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/trips")
@RequiredArgsConstructor
public class TripController {

    private final ITripService tripService; // Dùng final theo guideline
    private final TripMapper tripMapper;    // Inject Mapper vào Controller

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TripDetailedResponseDTO>>> getTrips(
            @RequestParam(required = false) String status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer originId,
            @RequestParam(required = false) Integer destinationId,
            // ---------------------------

            @PageableDefault(
                    page = 0,
                    size = 20,
                    sort = { "departureTime" },
                    direction = Sort.Direction.ASC
            ) Pageable pageable
    ) {
        // Truyền thêm originId và destinationId vào Service
        Page<Trip> tripPage = tripService.getTripsByFilters(status, date, originId, destinationId, pageable);

        Page<TripDetailedResponseDTO> responsePage = tripPage.map(tripMapper::toDetailedDTO);

        return ResponseEntity.ok(ApiResponse.success("Trips retrieved successfully", responsePage));
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
        // 1. Lấy List<Trip>
        List<Trip> trips = tripService.getTripsDetailsByDate(date);

        // 2. Map sang List<DTO>
        List<TripDetailedResponseDTO> responseDTOs = trips.stream()
                .map(tripMapper::toDetailedDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Trips retrieved successfully", responseDTOs));
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
        // 1. Service tạo và trả về Entity
        Trip newTrip = tripService.createTrip(request);

        // 2. Controller dùng Mapper chuyển Entity -> DTO trả về
        TripDetailedResponseDTO responseDTO = tripMapper.toDetailedDTO(newTrip);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Trip created successfully", responseDTO));
    }

    @DeleteMapping("/{tripId}")
    public ResponseEntity<ApiResponse<Void>> deleteTrip(@PathVariable Integer tripId) {
        tripService.deleteTrip(tripId);
        return ResponseEntity.ok(ApiResponse.success("Trip deleted successfully", null));
    }
}