package com.example.Fuba_BE.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.Fuba_BE.dto.Location.CreateLocationRequestDTO;
import com.example.Fuba_BE.dto.Location.LocationResponseDTO;
import com.example.Fuba_BE.dto.Location.UpdateLocationRequestDTO;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.service.Location.ILocationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/locations")
@RequiredArgsConstructor
@Slf4j
public class LocationController {

    private final ILocationService locationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LocationResponseDTO>>> getAllLocations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String province) {

        Pageable pageable = PageRequest.of(page, size);
        Page<LocationResponseDTO> locations = locationService.getLocations(pageable, search, province);

        return ResponseEntity.ok(
                ApiResponse.success("Locations retrieved successfully", locations.getContent()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LocationResponseDTO>> getLocationById(@PathVariable Integer id) {
        LocationResponseDTO location = locationService.getLocationById(id);
        return ResponseEntity.ok(
                ApiResponse.success("Location retrieved successfully", location));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LocationResponseDTO>> createLocation(
            @Valid @RequestBody CreateLocationRequestDTO request) {
        log.info("Creating new location: {}", request.getLocationName());
        LocationResponseDTO location = locationService.createLocation(request);
        return ResponseEntity.ok(
                ApiResponse.success("Location created successfully", location));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LocationResponseDTO>> updateLocation(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateLocationRequestDTO request) {
        log.info("Updating location with ID: {}", id);
        request.setLocationId(id);
        LocationResponseDTO location = locationService.updateLocation(request);
        return ResponseEntity.ok(
                ApiResponse.success("Location updated successfully", location));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteLocation(@PathVariable Integer id) {
        log.info("Deleting location with ID: {}", id);
        locationService.deleteLocation(id);
        return ResponseEntity.ok(
                ApiResponse.success("Location deleted successfully", null));
    }

    @GetMapping("/provinces")
    public ResponseEntity<ApiResponse<List<String>>> getProvinces() {
        List<String> provinces = locationService.getDistinctProvinces();
        return ResponseEntity.ok(
                ApiResponse.success("Provinces retrieved successfully", provinces));
    }
}
