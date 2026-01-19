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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.Fuba_BE.dto.Driver.CreateDriverWithAccountRequest;
import com.example.Fuba_BE.dto.Driver.DriverRequestDTO;
import com.example.Fuba_BE.dto.Driver.DriverResponseDTO;
import com.example.Fuba_BE.dto.Driver.DriverSelectionDTO;
import com.example.Fuba_BE.dto.Driver.DriverStatsDTO;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.service.Driver.IDriverService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for driver management with authorization and validation
 */
@RestController
@RequestMapping("/drivers")
@RequiredArgsConstructor
@Slf4j
public class DriverController {

    private final IDriverService driverService;

    /**
     * Get drivers for selection dropdown
     */
    @GetMapping("/selection")
    public ResponseEntity<ApiResponse<List<DriverSelectionDTO>>> getDriversForSelection() {
        List<DriverSelectionDTO> drivers = driverService.getAllDriversForSelection();
        return ResponseEntity.ok(ApiResponse.success("Driver selection list retrieved", drivers));
    }

    /**
     * Get driver statistics
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<DriverStatsDTO>> getDriverStats() {
        DriverStatsDTO stats = driverService.getDriverStats();
        return ResponseEntity.ok(ApiResponse.success("Driver statistics retrieved", stats));
    }

    /**
     * Get all drivers with pagination and search
     * ADMIN and STAFF can view drivers
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<DriverResponseDTO>>> getAllDrivers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer routeId,
            @PageableDefault(page = 0, size = 20, sort = "driverId", direction = Sort.Direction.DESC) Pageable pageable) {
        log.debug("📥 Request to get all drivers with keyword: {}, status: {}, routeId: {}", keyword, status, routeId);
        Page<DriverResponseDTO> drivers = driverService.getAllDrivers(keyword, status, routeId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Drivers retrieved successfully", drivers));
    }

    /**
     * Get driver by ID
     * ADMIN and STAFF can view driver details
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<DriverResponseDTO>> getDriverById(@PathVariable Integer id) {
        log.info("📥 Request to get driver by ID: {}", id);
        DriverResponseDTO driver = driverService.getDriverById(id);
        return ResponseEntity.ok(ApiResponse.success("Driver retrieved successfully", driver));
    }

    /**
     * Create new driver
     * Only ADMIN and STAFF can create drivers
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<DriverResponseDTO>> createDriver(
            @Valid @RequestBody DriverRequestDTO request) {
        log.info("📥 Request to create driver with license: {}", request.getDriverLicense());
        DriverResponseDTO createdDriver = driverService.createDriver(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Driver created successfully", createdDriver));
    }

    /**
     * Create new driver with user account and avatar
     * Only ADMIN and STAFF can create drivers
     * Accepts multipart/form-data with optional avatar file
     */
    @PostMapping(value = "/with-account", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<DriverResponseDTO>> createDriverWithAccount(
            @Valid @ModelAttribute CreateDriverWithAccountRequest request,
            @RequestPart(value = "avatar", required = false) MultipartFile avatarFile) {
        log.info("📥 Request to create driver with account - Email: {}, Has Avatar: {}",
                request.getEmail(), avatarFile != null && !avatarFile.isEmpty());
        DriverResponseDTO createdDriver = driverService.createDriverWithAccount(request, avatarFile);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Driver and account created successfully", createdDriver));
    }

    /**
     * Update driver
     * Only ADMIN and STAFF can update drivers
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<DriverResponseDTO>> updateDriver(
            @PathVariable Integer id,
            @Valid @RequestBody DriverRequestDTO request) {
        log.info("📥 Request to update driver ID: {}", id);
        DriverResponseDTO updatedDriver = driverService.updateDriver(id, request);
        return ResponseEntity.ok(ApiResponse.success("Driver updated successfully", updatedDriver));
    }

    /**
     * Delete driver
     * Only ADMIN can delete drivers
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteDriver(@PathVariable Integer id) {
        log.info("📥 Request to delete driver ID: {}", id);
        driverService.deleteDriver(id);
        return ResponseEntity.ok(ApiResponse.success("Driver deleted successfully", null));
    }
}
