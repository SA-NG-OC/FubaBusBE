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
import org.springframework.web.bind.annotation.RestController;

import com.example.Fuba_BE.dto.Vehicle.VehicleTypeRequestDTO;
import com.example.Fuba_BE.dto.Vehicle.VehicleTypeResponseDTO;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.service.Vehicle.IVehicleTypeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for vehicle type management
 * Manages different types of vehicles (Limousine, Giường nằm, etc.)
 */
@RestController
@RequestMapping("/vehicle-types")
@RequiredArgsConstructor
@Slf4j
public class VehicleTypeController {

    private final IVehicleTypeService vehicleTypeService;

    /**
     * Get all vehicle types for selection dropdown (no pagination)
     * Public endpoint - anyone can access
     */
    @GetMapping("/selection")
    public ResponseEntity<ApiResponse<List<VehicleTypeResponseDTO>>> getVehicleTypesForSelection() {
        log.debug("📥 Request to get vehicle types for selection");
        List<VehicleTypeResponseDTO> vehicleTypes = vehicleTypeService.getAllVehicleTypesForSelection();
        return ResponseEntity.ok(ApiResponse.success("Vehicle types retrieved successfully", vehicleTypes));
    }

    /**
     * Get all vehicle types with pagination
     * ADMIN and STAFF can view vehicle types
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<VehicleTypeResponseDTO>>> getAllVehicleTypes(
            @PageableDefault(page = 0, size = 20, sort = "typeName", direction = Sort.Direction.ASC) Pageable pageable) {
        log.debug("📥 Request to get all vehicle types with pagination");
        Page<VehicleTypeResponseDTO> vehicleTypes = vehicleTypeService.getAllVehicleTypes(pageable);
        return ResponseEntity.ok(ApiResponse.success("Vehicle types retrieved successfully", vehicleTypes));
    }

    /**
     * Get vehicle type by ID
     * ADMIN and STAFF can view vehicle type details
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<VehicleTypeResponseDTO>> getVehicleTypeById(@PathVariable Integer id) {
        log.info("📥 Request to get vehicle type by ID: {}", id);
        VehicleTypeResponseDTO vehicleType = vehicleTypeService.getVehicleTypeById(id);
        return ResponseEntity.ok(ApiResponse.success("Vehicle type retrieved successfully", vehicleType));
    }

    /**
     * Create new vehicle type
     * Only ADMIN can create vehicle types
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VehicleTypeResponseDTO>> createVehicleType(
            @Valid @RequestBody VehicleTypeRequestDTO request) {
        log.info("📥 Request to create vehicle type: {}", request.getTypeName());
        VehicleTypeResponseDTO createdVehicleType = vehicleTypeService.createVehicleType(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Vehicle type created successfully", createdVehicleType));
    }

    /**
     * Update vehicle type
     * Only ADMIN can update vehicle types
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VehicleTypeResponseDTO>> updateVehicleType(
            @PathVariable Integer id,
            @Valid @RequestBody VehicleTypeRequestDTO request) {
        log.info("📥 Request to update vehicle type ID: {}", id);
        VehicleTypeResponseDTO updatedVehicleType = vehicleTypeService.updateVehicleType(id, request);
        return ResponseEntity.ok(ApiResponse.success("Vehicle type updated successfully", updatedVehicleType));
    }

    /**
     * Delete vehicle type
     * Only ADMIN can delete vehicle types
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteVehicleType(@PathVariable Integer id) {
        log.info("📥 Request to delete vehicle type ID: {}", id);
        vehicleTypeService.deleteVehicleType(id);
        return ResponseEntity.ok(ApiResponse.success("Vehicle type deleted successfully", null));
    }
}
