package com.example.Fuba_BE.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.Fuba_BE.domain.entity.TripGenerationLog;
import com.example.Fuba_BE.dto.scheduling.TripGenerationRequest;
import com.example.Fuba_BE.dto.scheduling.TripGenerationResponse;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.repository.TripGenerationLogRepository;
import com.example.Fuba_BE.service.Scheduling.ITripGenerationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for Trip Generation
 * Main API for automatic trip creation from templates
 */
@RestController
@RequestMapping("/trips")
@RequiredArgsConstructor
@Slf4j
public class TripGenerationController {

    private final ITripGenerationService tripGenerationService;
    private final TripGenerationLogRepository generationLogRepository;

    /**
     * Generate trips from template (main endpoint)
     * POST /trips/generate-from-template
     * 
     * Example request:
     * {
     *   "templateId": 1,
     *   "startDate": "2026-02-01",
     *   "endDate": "2026-02-28",
     *   "autoAssignDrivers": true,
     *   "autoAssignVehicles": true,
     *   "respectWorkingHourLimit": true,
     *   "dryRun": false
     * }
     */
    @PostMapping("/generate-from-template")
    public ResponseEntity<ApiResponse<TripGenerationResponse>> generateTrips(
            @Valid @RequestBody TripGenerationRequest request) {
        
        log.info("Received trip generation request. Template: {}, Period: {} to {}, DryRun: {}",
                 request.getTemplateId(), request.getStartDate(), request.getEndDate(), request.getDryRun());

        try {
            TripGenerationResponse response = tripGenerationService.generateTripsFromTemplate(request);
            
            String message = request.getDryRun() 
                ? String.format("Preview: Would create %d trips", response.getCreatedTrips())
                : String.format("Successfully created %d trips", response.getCreatedTrips());

            log.info("Generation completed. Status: {}, Created: {}, Skipped: {}", 
                     response.getStatus(), response.getCreatedTrips(), response.getSkippedCount());

            return ResponseEntity.ok(ApiResponse.success(message, response));

        } catch (Exception e) {
            log.error("Trip generation failed", e);
            throw e;
        }
    }

    /**
     * Preview trip generation (dry-run)
     * POST /trips/preview-generation
     * 
     * Same as generate-from-template but automatically sets dryRun=true
     */
    @PostMapping("/preview-generation")
    public ResponseEntity<ApiResponse<TripGenerationResponse>> previewGeneration(
            @Valid @RequestBody TripGenerationRequest request) {
        
        log.info("Preview generation request. Template: {}, Period: {} to {}",
                 request.getTemplateId(), request.getStartDate(), request.getEndDate());

        request.setDryRun(true);
        TripGenerationResponse response = tripGenerationService.previewTripGeneration(request);

        return ResponseEntity.ok(ApiResponse.success(
            String.format("Preview: Would create %d trips, skip %d", 
                         response.getCreatedTrips(), response.getSkippedCount()),
            response
        ));
    }

    /**
     * Validate driver working hours for specific date
     * GET /trips/validate-driver-hours?driverId=10&date=2026-02-15&additionalHours=8.5
     */
    @GetMapping("/validate-driver-hours")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateDriverHours(
            @RequestParam Integer driverId,
            @RequestParam String date,
            @RequestParam double additionalHours) {
        
        LocalDate workDate = LocalDate.parse(date);
        double currentHours = tripGenerationService.calculateDriverHoursOnDate(driverId, workDate);
        boolean canWork = tripGenerationService.validateDriverWorkingHours(driverId, workDate, additionalHours);

        Map<String, Object> result = new HashMap<>();
        result.put("driverId", driverId);
        result.put("date", date);
        result.put("currentHours", currentHours);
        result.put("additionalHours", additionalHours);
        result.put("totalHours", currentHours + additionalHours);
        result.put("canWork", canWork);
        result.put("maxHours", 10.0);
        result.put("remainingHours", Math.max(0, 10.0 - currentHours));

        String message = canWork 
            ? "Driver can take this trip" 
            : "Driver would exceed 10-hour limit";

        return ResponseEntity.ok(ApiResponse.success(message, result));
    }

    /**
     * Calculate driver's total working hours on specific date
     * GET /trips/driver-hours?driverId=10&date=2026-02-15
     */
    @GetMapping("/driver-hours")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDriverHours(
            @RequestParam Integer driverId,
            @RequestParam String date) {
        
        LocalDate workDate = LocalDate.parse(date);
        double totalHours = tripGenerationService.calculateDriverHoursOnDate(driverId, workDate);

        Map<String, Object> result = new HashMap<>();
        result.put("driverId", driverId);
        result.put("date", date);
        result.put("totalHours", totalHours);
        result.put("maxHours", 10.0);
        result.put("remainingHours", Math.max(0, 10.0 - totalHours));
        result.put("utilizationPercent", (totalHours / 10.0) * 100);

        return ResponseEntity.ok(ApiResponse.success("Driver hours calculated", result));
    }

    /**
     * Get generation logs (audit trail)
     * GET /trips/generation-logs?page=0&size=20
     */
    @GetMapping("/generation-logs")
    public ResponseEntity<ApiResponse<Page<TripGenerationLog>>> getGenerationLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer templateId,
            @RequestParam(required = false) String status) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<TripGenerationLog> logs;

        if (templateId != null) {
            // Get list and convert to Page manually
            List<TripGenerationLog> list = generationLogRepository.findByTemplateId(templateId);
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), list.size());
            logs = new org.springframework.data.domain.PageImpl<>(list.subList(start, end), pageable, list.size());
        } else if (status != null) {
            List<TripGenerationLog> list = generationLogRepository.findByStatus(status);
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), list.size());
            logs = new org.springframework.data.domain.PageImpl<>(list.subList(start, end), pageable, list.size());
        } else {
            logs = generationLogRepository.findAll(pageable);
        }

        return ResponseEntity.ok(ApiResponse.success("Generation logs retrieved", logs));
    }

    /**
     * Get recent generation logs
     * GET /trips/generation-logs/recent?days=7
     */
    @GetMapping("/generation-logs/recent")
    public ResponseEntity<ApiResponse<List<TripGenerationLog>>> getRecentLogs(
            @RequestParam(defaultValue = "7") int days) {
        
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<TripGenerationLog> logs = generationLogRepository.findRecentLogs(since);

        return ResponseEntity.ok(ApiResponse.success("Recent logs retrieved", logs));
    }

    /**
     * Get generation statistics
     * GET /trips/generation-stats?days=30
     */
    @GetMapping("/generation-stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGenerationStatistics(
            @RequestParam(defaultValue = "30") int days) {
        
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        Object[] stats = generationLogRepository.getStatisticsSince(since);

        Map<String, Object> result = new HashMap<>();
        if (stats != null && stats.length >= 3) {
            result.put("periodDays", days);
            result.put("totalGenerations", stats[0]); // COUNT
            result.put("totalTripsCreated", stats[1]); // SUM
            result.put("averageTripsPerGeneration", stats[2]); // AVG
            result.put("since", since);
        } else {
            result.put("periodDays", days);
            result.put("totalGenerations", 0);
            result.put("totalTripsCreated", 0);
            result.put("averageTripsPerGeneration", 0.0);
            result.put("since", since);
        }

        return ResponseEntity.ok(ApiResponse.success("Statistics retrieved", result));
    }

    /**
     * Get generation log by ID
     * GET /trips/generation-logs/{id}
     */
    @GetMapping("/generation-logs/{id}")
    public ResponseEntity<ApiResponse<TripGenerationLog>> getGenerationLogById(@PathVariable Integer id) {
        TripGenerationLog log = generationLogRepository.findById(id)
            .orElseThrow(() -> new com.example.Fuba_BE.exception.NotFoundException(
                "Generation log not found with ID: " + id));

        return ResponseEntity.ok(ApiResponse.success("Log retrieved", log));
    }

    /**
     * Health check endpoint
     * GET /trips/generation/health
     */
    @GetMapping("/generation/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("service", "TripGenerationService");
        health.put("status", "UP");
        health.put("timestamp", java.time.LocalDateTime.now());
        health.put("features", List.of(
            "Round-trip generation",
            "Interval scheduling",
            "10-hour limit validation",
            "Conflict detection",
            "Priority-based assignment"
        ));

        return ResponseEntity.ok(ApiResponse.success("Service is healthy", health));
    }
}
