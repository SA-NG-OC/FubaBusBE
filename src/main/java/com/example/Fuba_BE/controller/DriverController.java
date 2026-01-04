package com.example.Fuba_BE.controller;

import com.example.Fuba_BE.dto.Driver.DriverSelectionDTO;
import com.example.Fuba_BE.service.Driver.IDriverService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final IDriverService driverService;

    @GetMapping("/selection")
    public ResponseEntity<List<DriverSelectionDTO>> getDriversForSelection() {
        return ResponseEntity.ok(driverService.getAllDriversForSelection());
    }
}
