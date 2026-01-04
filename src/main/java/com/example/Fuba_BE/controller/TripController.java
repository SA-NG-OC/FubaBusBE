package com.example.Fuba_BE.controller;

import com.example.Fuba_BE.domain.entity.Trip;
import com.example.Fuba_BE.dto.Trip.TripCalendarDTO;
import com.example.Fuba_BE.dto.Trip.TripDetailedResponseDTO;
import com.example.Fuba_BE.dto.Trip.TripStatusUpdateRequestDTO;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.service.Trip.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/trips")
@RequiredArgsConstructor
public class TripController {
    @Autowired
    private TripService tripService; // Gọi ông Quản lý ra để chờ lệnh

    @PostMapping
    public Trip createTrip(@RequestBody Trip trip) {
        // @RequestBody: Dịch bức thư JSON từ khách gửi thành Object Java
        return tripService.createTrip(trip);
    }

    @GetMapping("")
    public Page<TripDetailedResponseDTO> getTrips(
            @RequestParam(required = false) String status,
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = { "updatedAt", "createdAt" },
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return tripService.getTripsByStatus(status, pageable);
    }


    @GetMapping("/calendar-dates")
    public ResponseEntity<ApiResponse<List<LocalDate>>> getTripDates(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        List<LocalDate> dates = tripService.getDaysWithTrips(start, end);
        return ResponseEntity.ok(new ApiResponse<>(true, "Lấy danh sách ngày có chuyến thành công", dates));
    }

    @GetMapping("/by-date")
    public ResponseEntity<ApiResponse<List<TripDetailedResponseDTO>>> getTripsByDate(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<TripDetailedResponseDTO> trips = tripService.getTripsDetailsByDate(date);
        return ResponseEntity.ok(new ApiResponse<>(true, "Lấy danh sách chuyến theo ngày thành công", trips));
    }

    @PatchMapping("/{tripId}/status")
    public void updateTripStatus(
            @PathVariable Integer tripId,
            @Valid @RequestBody TripStatusUpdateRequestDTO request
    ) {
        tripService.updateTripStatus(tripId, request.getStatus());
    }

}
