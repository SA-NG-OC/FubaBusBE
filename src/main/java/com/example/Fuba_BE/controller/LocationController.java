package com.example.Fuba_BE.controller;

import com.example.Fuba_BE.dto.Location.LocationResponseDTO;
import com.example.Fuba_BE.service.Location.ILocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/locations")
@RequiredArgsConstructor
public class LocationController {

    private final ILocationService locationService;

    @GetMapping
    public List<LocationResponseDTO> getAllLocations() {
        return locationService.getAllLocations();
    }
}
