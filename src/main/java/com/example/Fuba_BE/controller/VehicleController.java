package com.example.Fuba_BE.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Fuba_BE.dto.Vehicle.VehicleSelectionDTO;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.service.Vehicle.IVehicleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/vehicles")
@RequiredArgsConstructor
@Tag(name = "Vehicle Management", description = "APIs for managing vehicles")
public class VehicleController {

    private final IVehicleService vehicleService;

    @GetMapping("/selection")
    @Operation(summary = "Get vehicles for selection dropdown", description = "Retrieve simplified vehicle list for dropdowns")
    public ResponseEntity<ApiResponse<List<VehicleSelectionDTO>>> getVehiclesForSelection() {
        List<VehicleSelectionDTO> vehicles = vehicleService.getAllVehiclesForSelection();
        return ResponseEntity.ok(ApiResponse.success("Vehicle selection list retrieved", vehicles));
    }
}