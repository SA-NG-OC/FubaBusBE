package com.example.Fuba_BE.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Fuba_BE.dto.Vehicle.VehicleSelectionDTO;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.service.Vehicle.IVehicleService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final IVehicleService vehicleService;

    @GetMapping("/selection")
    public ResponseEntity<ApiResponse<List<VehicleSelectionDTO>>> getVehiclesForSelection() {
        List<VehicleSelectionDTO> vehicles = vehicleService.getAllVehiclesForSelection();
        return ResponseEntity.ok(ApiResponse.success("Vehicle selection list retrieved", vehicles));
    }
}