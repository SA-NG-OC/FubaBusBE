package com.example.Fuba_BE.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Fuba_BE.dto.Driver.DriverSelectionDTO;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.service.Driver.IDriverService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/drivers")
@RequiredArgsConstructor
@Tag(name = "Driver Management", description = "APIs for managing drivers")
public class DriverController {

    private final IDriverService driverService;

    @GetMapping("/selection")
    @Operation(summary = "Get drivers for selection dropdown", description = "Retrieve simplified driver list for dropdowns")
    public ResponseEntity<ApiResponse<List<DriverSelectionDTO>>> getDriversForSelection() {
        List<DriverSelectionDTO> drivers = driverService.getAllDriversForSelection();
        return ResponseEntity.ok(ApiResponse.success("Driver selection list retrieved", drivers));
    }
}
