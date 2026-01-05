package com.example.Fuba_BE.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Fuba_BE.dto.Location.LocationResponseDTO;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.service.Location.ILocationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/locations")
@RequiredArgsConstructor
public class LocationController {

    private final ILocationService locationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LocationResponseDTO>>> getAllLocations() {
        List<LocationResponseDTO> locations = locationService.getAllLocations();
        return ResponseEntity.ok(
            ApiResponse.success("Locations retrieved successfully", locations)
        );
    }
}
