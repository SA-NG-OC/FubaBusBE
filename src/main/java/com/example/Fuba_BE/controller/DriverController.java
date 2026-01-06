package com.example.Fuba_BE.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Fuba_BE.dto.Driver.DriverSelectionDTO;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.service.Driver.IDriverService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final IDriverService driverService;

    @GetMapping("/selection")
    public ResponseEntity<ApiResponse<List<DriverSelectionDTO>>> getDriversForSelection() {
        List<DriverSelectionDTO> drivers = driverService.getAllDriversForSelection();
        return ResponseEntity.ok(ApiResponse.success("Driver selection list retrieved", drivers));
    }
}
