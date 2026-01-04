package com.example.Fuba_BE.controller;

import com.example.Fuba_BE.dto.Vehicle.VehicleSelectionDTO;
import com.example.Fuba_BE.service.Vehicle.IVehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final IVehicleService vehicleService;

    @GetMapping("/selection")
    public ResponseEntity<List<VehicleSelectionDTO>> getVehiclesForSelection() {
        return ResponseEntity.ok(vehicleService.getAllVehiclesForSelection());
    }
}