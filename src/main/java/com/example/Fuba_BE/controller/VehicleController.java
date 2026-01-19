package com.example.Fuba_BE.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.Fuba_BE.dto.Vehicle.VehicleRequestDTO;
import com.example.Fuba_BE.dto.Vehicle.VehicleResponseDTO;
import com.example.Fuba_BE.dto.Vehicle.VehicleSelectionDTO;
import com.example.Fuba_BE.dto.Vehicle.VehicleStatsDTO;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.service.Vehicle.IVehicleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for vehicle management with authorization and validation
 */
@RestController
@RequestMapping("/vehicles")
@RequiredArgsConstructor
@Slf4j
public class VehicleController {

    private final IVehicleService vehicleService;

    @GetMapping("/selection")
    public ResponseEntity<ApiResponse<List<VehicleSelectionDTO>>> getVehiclesForSelection() {
        List<VehicleSelectionDTO> vehicles = vehicleService.getAllVehiclesForSelection();
        return ResponseEntity.ok(ApiResponse.success("Vehicle selection list retrieved", vehicles));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<VehicleStatsDTO>> getVehicleStats() {
        VehicleStatsDTO stats = vehicleService.getVehicleStats();
        return ResponseEntity.ok(ApiResponse.success("Vehicle statistics retrieved", stats));
    }

    /**
     * Get all vehicles with pagination and search
     * ADMIN and STAFF can view vehicles
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<VehicleResponseDTO>>> getAllVehicles(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer routeId,
            @PageableDefault(page = 0, size = 20, sort = "vehicleId", direction = Sort.Direction.DESC) Pageable pageable) {
        log.debug("📥 Request to get all vehicles with keyword: {}, status: {}, routeId: {}", keyword, status, routeId);
        Page<VehicleResponseDTO> vehicles = vehicleService.getAllVehicles(keyword, status, routeId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Vehicles retrieved successfully", vehicles));
    }

    /**
     * Get vehicle by ID
     * ADMIN and STAFF can view vehicle details
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<VehicleResponseDTO>> getVehicleById(@PathVariable Integer id) {
        log.info("📥 Request to get vehicle by ID: {}", id);
        VehicleResponseDTO vehicle = vehicleService.getVehicleById(id);
        return ResponseEntity.ok(ApiResponse.success("Vehicle retrieved successfully", vehicle));
    }

    /**
     * Create new vehicle
     * Only ADMIN and STAFF can create vehicles
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<VehicleResponseDTO>> createVehicle(
            @Valid @RequestBody VehicleRequestDTO request) {
        log.info("📥 Request to create vehicle with license plate: {}", request.getLicensePlate());
        VehicleResponseDTO createdVehicle = vehicleService.createVehicle(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Vehicle created successfully", createdVehicle));
    }

    /**
     * Update vehicle
     * Only ADMIN and STAFF can update vehicles
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<VehicleResponseDTO>> updateVehicle(
            @PathVariable Integer id,
            @Valid @RequestBody VehicleRequestDTO request) {
        log.info("📥 Request to update vehicle ID: {}", id);
        VehicleResponseDTO updatedVehicle = vehicleService.updateVehicle(id, request);
        return ResponseEntity.ok(ApiResponse.success("Vehicle updated successfully", updatedVehicle));
    }

    /**
     * Delete vehicle
     * Only ADMIN can delete vehicles
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteVehicle(@PathVariable Integer id) {
        log.info("📥 Request to delete vehicle ID: {}", id);
        vehicleService.deleteVehicle(id);
        return ResponseEntity.ok(ApiResponse.success("Vehicle deleted successfully", null));
    }
}