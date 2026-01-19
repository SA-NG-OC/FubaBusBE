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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.Fuba_BE.domain.entity.DriverRouteAssignment;
import com.example.Fuba_BE.dto.scheduling.CreateDriverRouteAssignmentRequest;
import com.example.Fuba_BE.dto.scheduling.DriverRouteAssignmentResponse;
import com.example.Fuba_BE.exception.BadRequestException;
import com.example.Fuba_BE.exception.NotFoundException;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.repository.DriverRepository;
import com.example.Fuba_BE.repository.DriverRouteAssignmentRepository;
import com.example.Fuba_BE.repository.RouteRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for Driver-Route Assignments
 * Manages which drivers are assigned to which routes with priority
 */
@RestController
@RequestMapping("/driver-route-assignments")
@RequiredArgsConstructor
@Slf4j
public class DriverRouteAssignmentController {

    private final DriverRouteAssignmentRepository assignmentRepository;
    private final DriverRepository driverRepository;
    private final RouteRepository routeRepository;

    /**
     * Create driver-route assignment
     * POST /driver-route-assignments
     */
    @PostMapping
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<ApiResponse<DriverRouteAssignmentResponse>> createAssignment(
            @Valid @RequestBody CreateDriverRouteAssignmentRequest request) {

        log.info("Creating driver-route assignment. Driver: {}, Route: {}",
                request.getDriverId(), request.getRouteId());

        // Validate driver exists (with User fetch to avoid lazy loading)
        var driver = driverRepository.findByIdWithUser(request.getDriverId())
                .orElseThrow(() -> new NotFoundException("Driver not found with ID: " + request.getDriverId()));

        // Validate route exists (with origin/destination to avoid
        // LazyInitializationException)
        var route = routeRepository.findByIdWithLocations(request.getRouteId())
                .orElseThrow(() -> new NotFoundException("Route not found with ID: " + request.getRouteId()));

        // Check for overlapping assignments
        boolean hasOverlap = assignmentRepository.hasOverlappingAssignment(
                request.getDriverId(),
                request.getRouteId(),
                request.getStartDate(),
                request.getEndDate() != null ? request.getEndDate() : LocalDate.of(9999, 12, 31));

        if (hasOverlap) {
            throw new BadRequestException("Driver already has an overlapping assignment for this route");
        }

        // Create assignment
        DriverRouteAssignment assignment = DriverRouteAssignment.builder()
                .driver(driver)
                .route(route)
                .preferredRole(request.getPreferredRole())
                .priority(request.getPriority())
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
     * GET /driver-route-assignments?page=0&size=20
     */
    @GetMapping
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Page<DriverRouteAssignmentResponse>>> getAllAssignments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<DriverRouteAssignment> assignments = assignmentRepository.findAll(pageable);
        Page<DriverRouteAssignmentResponse> response = assignments.map(this::mapToResponse);

        return ResponseEntity.ok(ApiResponse.success("Assignments retrieved", response));
    }

    /**
     * Get assignments by driver
     * GET /driver-route-assignments/driver/{driverId}
     */
    @GetMapping("/driver/{driverId}")
    public ResponseEntity<ApiResponse<List<DriverRouteAssignmentResponse>>> getByDriver(
            @PathVariable Integer driverId,
            @RequestParam(required = false) String date) {

        List<DriverRouteAssignment> assignments;

        if (date != null) {
            LocalDate specificDate = LocalDate.parse(date);
            assignments = assignmentRepository.findByDriverIdAndDate(driverId, specificDate);
        } else {
            assignments = assignmentRepository.findByDriverDriverId(driverId);
        }

        List<DriverRouteAssignmentResponse> response = assignments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Driver assignments retrieved", response));
    }

    /**
     * Get assignments by route with priority order
     * GET /driver-route-assignments/route/{routeId}?date=2026-02-15
     */
    @GetMapping("/route/{routeId}")
    public ResponseEntity<ApiResponse<List<DriverRouteAssignmentResponse>>> getByRoute(
            @PathVariable Integer routeId,
            @RequestParam(required = false) String date) {

        List<DriverRouteAssignment> assignments;

        if (date != null) {
            LocalDate specificDate = LocalDate.parse(date);
            assignments = assignmentRepository.findEffectiveByRouteAndDate(routeId, specificDate);
        } else {
            assignments = assignmentRepository.findByRouteRouteIdOrderByPriorityAsc(routeId);
        }

        List<DriverRouteAssignmentResponse> response = assignments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Route assignments retrieved", response));
    }

    /**
     * Get assignment by ID
     * GET /driver-route-assignments/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DriverRouteAssignmentResponse>> getById(@PathVariable Integer id) {
        DriverRouteAssignment assignment = assignmentRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new NotFoundException("Assignment not found with ID: " + id));

        return ResponseEntity.ok(ApiResponse.success("Assignment retrieved", mapToResponse(assignment)));
    }

    /**
     * Update assignment
     * PUT /driver-route-assignments/{id}
     */
    @PutMapping("/{id}")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<ApiResponse<DriverRouteAssignmentResponse>> updateAssignment(
            @PathVariable Integer id,
            @Valid @RequestBody CreateDriverRouteAssignmentRequest request) {

        log.info("Updating assignment ID: {}", id);

        DriverRouteAssignment assignment = assignmentRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new NotFoundException("Assignment not found with ID: " + id));

        // Validate driver if changed
        if (!assignment.getDriver().getDriverId().equals(request.getDriverId())) {
            var driver = driverRepository.findById(request.getDriverId())
                    .orElseThrow(() -> new NotFoundException("Driver not found with ID: " + request.getDriverId()));
            assignment.setDriver(driver);
        }

        // Validate route if changed
        if (!assignment.getRoute().getRouteId().equals(request.getRouteId())) {
            var route = routeRepository.findById(request.getRouteId())
                    .orElseThrow(() -> new NotFoundException("Route not found with ID: " + request.getRouteId()));
            assignment.setRoute(route);
        }

        // Update fields
        assignment.setPreferredRole(request.getPreferredRole());
        assignment.setPriority(request.getPriority());
        assignment.setStartDate(request.getStartDate());
        assignment.setEndDate(request.getEndDate());
        assignment.setNotes(request.getNotes());

        assignment = assignmentRepository.save(assignment);
        log.info("Updated assignment ID: {}", id);

        return ResponseEntity.ok(ApiResponse.success("Assignment updated", mapToResponse(assignment)));
    }

    /**
     * Delete assignment
     * DELETE /driver-route-assignments/{id}
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
     * Get available drivers for route on specific date
     * GET /driver-route-assignments/available-drivers?routeId=1&date=2026-02-15
     */
    @GetMapping("/available-drivers")
    public ResponseEntity<ApiResponse<List<DriverRouteAssignmentResponse>>> getAvailableDrivers(
            @RequestParam Integer routeId,
            @RequestParam String date) {

        LocalDate specificDate = LocalDate.parse(date);
        List<DriverRouteAssignment> assignments = assignmentRepository.findEffectiveByRouteAndDate(routeId,
                specificDate);

        List<DriverRouteAssignmentResponse> response = assignments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(
                String.format("Found %d available drivers", response.size()),
                response));
    }

    // ========== MAPPER ==========

    private DriverRouteAssignmentResponse mapToResponse(DriverRouteAssignment assignment) {
        var driver = assignment.getDriver();
        var route = assignment.getRoute();

        return DriverRouteAssignmentResponse.builder()
                .assignmentId(assignment.getAssignmentId())
                .driverId(driver.getDriverId())
                .driverName(driver.getUser().getFullName())
                .routeId(route.getRouteId())
                .routeName(route.getRouteName())
                .originName(route.getOrigin().getLocationName())
                .destinationName(route.getDestination().getLocationName())
                .preferredRole(assignment.getPreferredRole())
                .priority(assignment.getPriority())
                .isActive(assignment.getIsActive())
                .startDate(assignment.getStartDate())
                .endDate(assignment.getEndDate())
                .notes(assignment.getNotes())
                .build();
    }
}
