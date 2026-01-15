package com.example.Fuba_BE.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import com.example.Fuba_BE.dto.Trip.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.Fuba_BE.domain.entity.Trip;
import com.example.Fuba_BE.mapper.TripMapper;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.service.Trip.ITripService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/trips")
@RequiredArgsConstructor
public class TripController {

    private final ITripService tripService;
    private final TripMapper tripMapper;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TripDetailedResponseDTO>>> getAllTrips(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "departureTime") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,

            // Filters
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer originId,
            @RequestParam(required = false) Integer destId,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,

            // Advanced Filters
            @RequestParam(required = false) List<String> timeRanges,
            @RequestParam(required = false) List<String> vehicleTypes
    ) {
        Page<TripDetailedResponseDTO> tripPage = tripService.getAllTrips(
                page, size, sortBy, sortDir, search, originId, destId, minPrice, maxPrice, date, timeRanges, vehicleTypes
        );

        // Enrich thêm thông tin stats (Booked/CheckedIn) nếu cần thiết
        // (Tùy chọn: Nếu muốn hiển thị số ghế booked ngay ở danh sách thì uncomment dòng dưới)
        // tripPage.forEach(dto -> tripService.enrichTripStats(dto, dto.getTripId()));

        return ResponseEntity.ok(ApiResponse.success("Trips retrieved successfully", tripPage));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TripDetailedResponseDTO>> createTrip(
            @Valid @RequestBody TripCreateRequestDTO request
    ) {
        Trip newTrip = tripService.createTrip(request);
        TripDetailedResponseDTO responseDTO = tripMapper.toDetailedDTO(newTrip);

        // Enrich stats cho chuyến mới (0 booked)
        responseDTO = tripService.enrichTripStats(responseDTO, newTrip.getTripId());
        if (newTrip.getVehicle() != null && newTrip.getVehicle().getVehicleType() != null) {
            responseDTO.setTotalSeats(newTrip.getVehicle().getVehicleType().getTotalSeats());
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Trip created successfully", responseDTO));
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
        List<Trip> trips = tripService.getTripsDetailsByDate(date);
        List<TripDetailedResponseDTO> responseDTOs = trips.stream()
                .map(tripMapper::toDetailedDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Trips retrieved successfully", responseDTOs));
    }

    @PatchMapping("/{tripId}/status")
    public ResponseEntity<ApiResponse<Void>> updateTripStatus(
            @PathVariable Integer tripId,
            @RequestBody @Valid TripStatusUpdateDTO request
    ) {
        tripService.updateTripStatus(tripId, request.getStatus(), request.getNote());
        return ResponseEntity.ok(ApiResponse.success("Trip status updated successfully", null));
    }

    @DeleteMapping("/{tripId}")
    public ResponseEntity<ApiResponse<Void>> deleteTrip(@PathVariable Integer tripId) {
        tripService.deleteTrip(tripId);
        return ResponseEntity.ok(ApiResponse.success("Trip deleted successfully", null));
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<ApiResponse<Page<TripDetailedResponseDTO>>> getDriverTrips(
            @PathVariable Integer driverId,
            @RequestParam(required = false) String status,
            @PageableDefault(page = 0, size = 10, sort = "departureTime", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<Trip> tripPage = tripService.getTripsForDriver(driverId, status, pageable);
        Page<TripDetailedResponseDTO> responsePage = tripPage.map(tripMapper::toDetailedDTO);
        return ResponseEntity.ok(ApiResponse.success("Driver trips retrieved successfully", responsePage));
    }

    @GetMapping("/{tripId}/passengers")
    public ResponseEntity<ApiResponse<List<PassengerOnTripResponseDTO>>> getPassengersOnTrip(
            @PathVariable Integer tripId
    ) {
        List<PassengerOnTripResponseDTO> passengers = tripService.getPassengersOnTrip(tripId);
        return ResponseEntity.ok(ApiResponse.success("Passengers on trip retrieved successfully", passengers));
    }

    @PutMapping("/{tripId}")
    public ResponseEntity<ApiResponse<TripDetailedResponseDTO>> updateTrip(
            @PathVariable Integer tripId,
            @Valid @RequestBody TripUpdateRequestDTO request
    ) {
        Trip updatedTrip = tripService.updateTrip(tripId, request);
        TripDetailedResponseDTO responseDTO = tripMapper.toDetailedDTO(updatedTrip);

        // Enrich stats lại để trả về data đầy đủ
        responseDTO = tripService.enrichTripStats(responseDTO, tripId);

        // Set total seats thủ công nếu mapper chưa làm
        if (updatedTrip.getVehicle() != null && updatedTrip.getVehicle().getVehicleType() != null) {
            responseDTO.setTotalSeats(updatedTrip.getVehicle().getVehicleType().getTotalSeats());
        }

        return ResponseEntity.ok(ApiResponse.success("Trip updated successfully", responseDTO));
    }
}