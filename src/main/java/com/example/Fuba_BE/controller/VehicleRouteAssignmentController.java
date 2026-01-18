package com.example.Fuba_BE.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.Fuba_BE.domain.entity.VehicleRouteAssignment;
import com.example.Fuba_BE.dto.scheduling.CreateVehicleRouteAssignmentRequest;
import com.example.Fuba_BE.dto.scheduling.VehicleRouteAssignmentResponse;
import com.example.Fuba_BE.exception.BadRequestException;
import com.example.Fuba_BE.exception.NotFoundException;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.repository.RouteRepository;
import com.example.Fuba_BE.repository.VehicleRepository;
import com.example.Fuba_BE.repository.VehicleRouteAssignmentRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for Vehicle-Route Assignments
 * Manages which vehicles are assigned to which routes with priority
 */
@RestController
@RequestMapping("/vehicle-route-assignments")
@RequiredArgsConstructor
@Slf4j
public class VehicleRouteAssignmentController {

    private final VehicleRouteAssignmentRepository assignmentRepository;
    private final VehicleRepository vehicleRepository;
    private final RouteRepository routeRepository;

    /**
     * Create vehicle-route assignment
     * POST /vehicle-route-assignments
     */
    @PostMapping
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<ApiResponse<VehicleRouteAssignmentResponse>> createAssignment(
            @Valid @RequestBody CreateVehicleRouteAssignmentRequest request) {

        log.info("Creating vehicle-route assignment. Vehicle: {}, Route: {}",
                request.getVehicleId(), request.getRouteId());

        // Validate vehicle exists
        var vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new NotFoundException("Vehicle not found with ID: " + request.getVehicleId()));

        // Validate route exists
        var route = routeRepository.findById(request.getRouteId())
                .orElseThrow(() -> new NotFoundException("Route not found with ID: " + request.getRouteId()));

        // Check for overlapping assignments
        boolean hasOverlap = assignmentRepository.hasOverlappingAssignment(
                request.getVehicleId(),
                request.getRouteId(),
                request.getStartDate(),
                request.getEndDate() != null ? request.getEndDate() : LocalDate.of(9999, 12, 31));

        if (hasOverlap) {
            throw new BadRequestException("Vehicle already has an overlapping assignment for this route");
        }

        // Create assignment
        VehicleRouteAssignment assignment = VehicleRouteAssignment.builder()
                .vehicle(vehicle)
                .route(route)
                .priority(request.getPriority())
                .maintenanceSchedule(request.getMaintenanceSchedule())
                .lastMaintenanceDate(request.getLastMaintenanceDate())
                .nextMaintenanceDate(request.getNextMaintenanceDate())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .notes(request.getNotes())
                .build();

        assignment = assignmentRepository.save(assignment);
        log.info("Created assignment with ID: {}", assignment.getAssignmentId());

        return ResponseEntity.ok(ApiResponse.success("Assignment created", mapToResponse(assignment)));
    }

    /**
     * Get all assignments with pagination
     * GET /vehicle-route-assignments?page=0&size=20
     */
    @GetMapping
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Page<VehicleRouteAssignmentResponse>>> getAllAssignments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<VehicleRouteAssignment> assignments = assignmentRepository.findAll(pageable);
        Page<VehicleRouteAssignmentResponse> response = assignments.map(this::mapToResponse);

        return ResponseEntity.ok(ApiResponse.success("Assignments retrieved", response));
    }

    /**
     * Get assignments by vehicle
     * GET /vehicle-route-assignments/vehicle/{vehicleId}
     */
    @GetMapping("/vehicle/{vehicleId}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<VehicleRouteAssignmentResponse>>> getByVehicle(
            @PathVariable Integer vehicleId,
            @RequestParam(required = false) String date) {

        List<VehicleRouteAssignment> assignments;

        if (date != null) {
            LocalDate specificDate = LocalDate.parse(date);
            assignments = assignmentRepository.findByVehicleIdAndDate(vehicleId, specificDate);
        } else {
            assignments = assignmentRepository.findByVehicleVehicleId(vehicleId);
        }

        List<VehicleRouteAssignmentResponse> response = assignments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Vehicle assignments retrieved", response));
    }

    /**
     * Get assignments by route with priority order
     * GET /vehicle-route-assignments/route/{routeId}?date=2026-02-15
     */
    @GetMapping("/route/{routeId}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<VehicleRouteAssignmentResponse>>> getByRoute(
            @PathVariable Integer routeId,
            @RequestParam(required = false) String date) {

        List<VehicleRouteAssignment> assignments;

        if (date != null) {
            LocalDate specificDate = LocalDate.parse(date);
            assignments = assignmentRepository.findEffectiveByRouteAndDate(routeId, specificDate);
        } else {
            assignments = assignmentRepository.findByRouteRouteIdOrderByPriorityAsc(routeId);
        }

        List<VehicleRouteAssignmentResponse> response = assignments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Route assignments retrieved", response));
    }

    /**
     * Get vehicles needing maintenance
     * GET /vehicle-route-assignments/needs-maintenance
     */
    @GetMapping("/needs-maintenance")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<VehicleRouteAssignmentResponse>>> getNeedingMaintenance() {
        List<VehicleRouteAssignment> assignments = assignmentRepository
                .findByNextMaintenanceDateBeforeOrEqual(LocalDate.now());

        List<VehicleRouteAssignmentResponse> response = assignments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(
                String.format("Found %d vehicles needing maintenance", response.size()),
                response));
    }

    /**
     * Get assignment by ID
     * GET /vehicle-route-assignments/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleRouteAssignmentResponse>> getById(@PathVariable Integer id) {
        VehicleRouteAssignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Assignment not found with ID: " + id));

        return ResponseEntity.ok(ApiResponse.success("Assignment retrieved", mapToResponse(assignment)));
    }

    /**
     * Update assignment
     * PUT /vehicle-route-assignments/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleRouteAssignmentResponse>> updateAssignment(
            @PathVariable Integer id,
            @Valid @RequestBody CreateVehicleRouteAssignmentRequest request) {

        log.info("Updating assignment ID: {}", id);

        VehicleRouteAssignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Assignment not found with ID: " + id));

        // Validate vehicle if changed
        if (!assignment.getVehicle().getVehicleId().equals(request.getVehicleId())) {
            var vehicle = vehicleRepository.findById(request.getVehicleId())
                    .orElseThrow(() -> new NotFoundException("Vehicle not found with ID: " + request.getVehicleId()));
            assignment.setVehicle(vehicle);
        }

        // Validate route if changed
        if (!assignment.getRoute().getRouteId().equals(request.getRouteId())) {
            var route = routeRepository.findById(request.getRouteId())
                    .orElseThrow(() -> new NotFoundException("Route not found with ID: " + request.getRouteId()));
            assignment.setRoute(route);
        }

        // Update fields
        assignment.setPriority(request.getPriority());
        assignment.setMaintenanceSchedule(request.getMaintenanceSchedule());
        assignment.setLastMaintenanceDate(request.getLastMaintenanceDate());
        assignment.setNextMaintenanceDate(request.getNextMaintenanceDate());
        assignment.setStartDate(request.getStartDate());
        assignment.setEndDate(request.getEndDate());
        assignment.setNotes(request.getNotes());

        assignment = assignmentRepository.save(assignment);
        log.info("Updated assignment ID: {}", id);

        return ResponseEntity.ok(ApiResponse.success("Assignment updated", mapToResponse(assignment)));
    }

    /**
     * Update maintenance information
     * PATCH /vehicle-route-assignments/{id}/maintenance
     */
    @PatchMapping("/{id}/maintenance")
    public ResponseEntity<ApiResponse<VehicleRouteAssignmentResponse>> updateMaintenance(
            @PathVariable Integer id,
            @RequestParam String lastDate,
            @RequestParam(required = false) String nextDue) {

        VehicleRouteAssignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Assignment not found with ID: " + id));

        assignment.setLastMaintenanceDate(LocalDate.parse(lastDate));
        if (nextDue != null) {
            assignment.setNextMaintenanceDate(LocalDate.parse(nextDue));
        }

        assignment = assignmentRepository.save(assignment);
        log.info("Updated maintenance for assignment ID: {}", id);

        return ResponseEntity.ok(ApiResponse.success("Maintenance updated", mapToResponse(assignment)));
    }

    /**
     * Delete assignment
     * DELETE /vehicle-route-assignments/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAssignment(@PathVariable Integer id) {
        log.info("Deleting assignment ID: {}", id);

        if (!assignmentRepository.existsById(id)) {
            throw new NotFoundException("Assignment not found with ID: " + id);
        }

        assignmentRepository.deleteById(id);
        log.info("Deleted assignment ID: {}", id);

        return ResponseEntity.ok(ApiResponse.success("Assignment deleted", null));
    }

    /**
     * Get available vehicles for route on specific date
     * GET /vehicle-route-assignments/available-vehicles?routeId=1&date=2026-02-15
     */
    @GetMapping("/available-vehicles")
    public ResponseEntity<ApiResponse<List<VehicleRouteAssignmentResponse>>> getAvailableVehicles(
            @RequestParam Integer routeId,
            @RequestParam String date) {

        LocalDate specificDate = LocalDate.parse(date);
        List<VehicleRouteAssignment> assignments = assignmentRepository.findEffectiveByRouteAndDate(routeId,
                specificDate);

        // Filter out vehicles needing maintenance
        List<VehicleRouteAssignmentResponse> response = assignments.stream()
                .filter(a -> !a.needsMaintenance())
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(
                String.format("Found %d available vehicles", response.size()),
                response));
    }

    // ========== MAPPER ==========

    private VehicleRouteAssignmentResponse mapToResponse(VehicleRouteAssignment assignment) {
        var vehicle = assignment.getVehicle();
        var route = assignment.getRoute();

        return VehicleRouteAssignmentResponse.builder()
                .assignmentId(assignment.getAssignmentId())
                .vehicleId(vehicle.getVehicleId())
                .vehicleLicensePlate(vehicle.getLicensePlate())
                .vehicleType(vehicle.getVehicleType().getTypeName())
                .totalSeats(vehicle.getVehicleType().getTotalSeats())
                .routeId(route.getRouteId())
                .routeName(route.getRouteName())
                .originName(route.getOrigin().getLocationName())
                .destinationName(route.getDestination().getLocationName())
                .priority(assignment.getPriority())
                .isActive(assignment.getIsActive())
                .startDate(assignment.getStartDate())
                .endDate(assignment.getEndDate())
                .maintenanceSchedule(assignment.getMaintenanceSchedule())
                .nextMaintenanceDate(assignment.getNextMaintenanceDate())
                .needsMaintenance(assignment.needsMaintenance())
                .notes(assignment.getNotes())
                .build();
    }
}
