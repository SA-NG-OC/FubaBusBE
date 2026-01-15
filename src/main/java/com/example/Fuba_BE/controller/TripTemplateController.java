package com.example.Fuba_BE.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

import com.example.Fuba_BE.domain.entity.TripTemplate;
import com.example.Fuba_BE.dto.scheduling.CreateTripTemplateRequest;
import com.example.Fuba_BE.dto.scheduling.TripTemplateResponse;
import com.example.Fuba_BE.exception.BadRequestException;
import com.example.Fuba_BE.exception.NotFoundException;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.repository.RouteRepository;
import com.example.Fuba_BE.repository.TripTemplateRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for Trip Template management
 * Supports CRUD operations and template preview
 */
@RestController
@RequestMapping("/trip-templates")
@RequiredArgsConstructor
@Slf4j
public class TripTemplateController {

    private final TripTemplateRepository templateRepository;
    private final RouteRepository routeRepository;

    /**
     * Create new trip template
     * POST /trip-templates
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TripTemplateResponse>> createTemplate(
            @Valid @RequestBody CreateTripTemplateRequest request) {
        
        log.info("Creating trip template: {}", request.getTemplateName());

        // Validate route exists
        var route = routeRepository.findById(request.getRouteId())
            .orElseThrow(() -> new NotFoundException("Route not found with ID: " + request.getRouteId()));

        // Check for duplicate template
        boolean exists = templateRepository.existsDuplicateTemplate(
            request.getRouteId(),
            request.getDepartureTime(),
            request.getDaysOfWeek(),
            request.getEffectiveFrom(),
            null // templateId is null for new templates
        );

        if (exists) {
            throw new BadRequestException("Template with same route, time, and days already exists");
        }

        // Create template entity
        TripTemplate template = TripTemplate.builder()
            .templateName(request.getTemplateName())
            .route(route)
            .departureTime(request.getDepartureTime())
            .daysOfWeek(request.getDaysOfWeek())
            .basePrice(request.getBasePrice())
            .onlineBookingCutoff(request.getOnlineBookingCutoff())
            .minPassengers(request.getMinPassengers())
            .maxPassengers(request.getMaxPassengers())
            .generateRoundTrip(request.getGenerateRoundTrip())
            .intervalMinutes(request.getIntervalMinutes())
            .tripsPerDay(request.getTripsPerDay())
            .maxGenerationDays(request.getMaxGenerationDays())
            .autoAssignDriver(request.getAutoAssignDriver())
            .autoAssignVehicle(request.getAutoAssignVehicle())
            .autoCancelIfNotEnough(request.getAutoCancelIfNotEnough())
            .isActive(true)
            .effectiveFrom(request.getEffectiveFrom())
            .effectiveTo(request.getEffectiveTo())
            .notes(request.getNotes())
            .build();

        template = templateRepository.save(template);
        log.info("Created template with ID: {}", template.getTemplateId());

        return ResponseEntity.ok(ApiResponse.success(
            "Template created successfully",
            mapToResponse(template)
        ));
    }

    /**
     * Get all templates with pagination
     * GET /trip-templates?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<TripTemplateResponse>>> getAllTemplates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean activeOnly) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("templateId").descending());
        Page<TripTemplate> templates;

        if (Boolean.TRUE.equals(activeOnly)) {
            // Get list and convert to Page manually
            List<TripTemplate> list = templateRepository.findAllActive();
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), list.size());
            templates = new org.springframework.data.domain.PageImpl<>(list, pageable, list.size());
        } else {
            templates = templateRepository.findAll(pageable);
        }

        Page<TripTemplateResponse> response = templates.map(this::mapToResponse);
        return ResponseEntity.ok(ApiResponse.success("Templates retrieved", response));
    }

    /**
     * Get template by ID
     * GET /trip-templates/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TripTemplateResponse>> getTemplateById(@PathVariable Integer id) {
        TripTemplate template = templateRepository.findByIdWithRoute(id)
            .orElseThrow(() -> new NotFoundException("Template not found with ID: " + id));

        return ResponseEntity.ok(ApiResponse.success("Template retrieved", mapToResponse(template)));
    }

    /**
     * Get templates by route
     * GET /trip-templates/route/{routeId}
     */
    @GetMapping("/route/{routeId}")
    public ResponseEntity<ApiResponse<List<TripTemplateResponse>>> getTemplatesByRoute(
            @PathVariable Integer routeId,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        
        List<TripTemplate> templates;
        if (activeOnly) {
            templates = templateRepository.findActiveByRouteId(routeId);
        } else {
            templates = templateRepository.findAll().stream()
                .filter(t -> t.getRoute().getRouteId().equals(routeId))
                .collect(Collectors.toList());
        }

        List<TripTemplateResponse> response = templates.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Templates retrieved", response));
    }

    /**
     * Get templates effective on specific date
     * GET /trip-templates/effective?date=2026-02-15
     */
    @GetMapping("/effective")
    public ResponseEntity<ApiResponse<List<TripTemplateResponse>>> getEffectiveTemplates(
            @RequestParam String date) {
        
        java.time.LocalDate localDate = java.time.LocalDate.parse(date);
        List<TripTemplate> templates = templateRepository.findEffectiveOnDate(localDate);

        List<TripTemplateResponse> response = templates.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Effective templates retrieved", response));
    }

    /**
     * Update template
     * PUT /trip-templates/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TripTemplateResponse>> updateTemplate(
            @PathVariable Integer id,
            @Valid @RequestBody CreateTripTemplateRequest request) {
        
        log.info("Updating template ID: {}", id);

        TripTemplate template = templateRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Template not found with ID: " + id));

        // Validate route if changed
        if (!template.getRoute().getRouteId().equals(request.getRouteId())) {
            var route = routeRepository.findById(request.getRouteId())
                .orElseThrow(() -> new NotFoundException("Route not found with ID: " + request.getRouteId()));
            template.setRoute(route);
        }

        // Update fields
        template.setTemplateName(request.getTemplateName());
        template.setDepartureTime(request.getDepartureTime());
        template.setDaysOfWeek(request.getDaysOfWeek());
        template.setBasePrice(request.getBasePrice());
        template.setOnlineBookingCutoff(request.getOnlineBookingCutoff());
        template.setMinPassengers(request.getMinPassengers());
        template.setMaxPassengers(request.getMaxPassengers());
        template.setGenerateRoundTrip(request.getGenerateRoundTrip());
        template.setIntervalMinutes(request.getIntervalMinutes());
        template.setTripsPerDay(request.getTripsPerDay());
        template.setMaxGenerationDays(request.getMaxGenerationDays());
        template.setAutoAssignDriver(request.getAutoAssignDriver());
        template.setAutoAssignVehicle(request.getAutoAssignVehicle());
        template.setAutoCancelIfNotEnough(request.getAutoCancelIfNotEnough());
        template.setEffectiveFrom(request.getEffectiveFrom());
        template.setEffectiveTo(request.getEffectiveTo());
        template.setNotes(request.getNotes());

        template = templateRepository.save(template);
        log.info("Updated template ID: {}", id);

        return ResponseEntity.ok(ApiResponse.success("Template updated", mapToResponse(template)));
    }

    /**
     * Activate/deactivate template
     * PATCH /trip-templates/{id}/status?active=true
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<TripTemplateResponse>> updateTemplateStatus(
            @PathVariable Integer id,
            @RequestParam boolean active) {
        
        TripTemplate template = templateRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Template not found with ID: " + id));

        template.setIsActive(active);
        template = templateRepository.save(template);

        String message = active ? "Template activated" : "Template deactivated";
        return ResponseEntity.ok(ApiResponse.success(message, mapToResponse(template)));
    }

    /**
     * Delete template
     * DELETE /trip-templates/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable Integer id) {
        log.info("Deleting template ID: {}", id);

        if (!templateRepository.existsById(id)) {
            throw new NotFoundException("Template not found with ID: " + id);
        }

        templateRepository.deleteById(id);
        log.info("Deleted template ID: {}", id);

        return ResponseEntity.ok(ApiResponse.success("Template deleted", null));
    }

    /**
     * Get templates expiring soon
     * GET /trip-templates/expiring?days=7
     */
    @GetMapping("/expiring")
    public ResponseEntity<ApiResponse<List<TripTemplateResponse>>> getExpiringSoon(
            @RequestParam(defaultValue = "7") int days) {
        
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(days);
        List<TripTemplate> templates = templateRepository.findExpiringSoon(startDate, endDate);
        List<TripTemplateResponse> response = templates.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Expiring templates retrieved", response));
    }

    // ========== MAPPER ==========

    private TripTemplateResponse mapToResponse(TripTemplate template) {
        var route = template.getRoute();
        
        return TripTemplateResponse.builder()
            .templateId(template.getTemplateId())
            .templateName(template.getTemplateName())
            .routeId(route.getRouteId())
            .routeName(route.getRouteName())
            .originName(route.getOrigin().getLocationName())
            .destinationName(route.getDestination().getLocationName())
            .estimatedDuration(route.getEstimatedDuration())
            .departureTime(template.getDepartureTime())
            .daysOfWeek(template.getDaysOfWeek())
            .daysList(template.getDaysList())
            .basePrice(template.getBasePrice())
            .onlineBookingCutoff(template.getOnlineBookingCutoff())
            .minPassengers(template.getMinPassengers())
            .maxPassengers(template.getMaxPassengers())
            .generateRoundTrip(template.getGenerateRoundTrip())
            .intervalMinutes(template.getIntervalMinutes())
            .tripsPerDay(template.getTripsPerDay())
            .totalTripsPerDay(template.getTotalTripsPerDay())
            .maxGenerationDays(template.getMaxGenerationDays())
            .autoAssignDriver(template.getAutoAssignDriver())
            .autoAssignVehicle(template.getAutoAssignVehicle())
            .autoCancelIfNotEnough(template.getAutoCancelIfNotEnough())
            .isActive(template.getIsActive())
            .effectiveFrom(template.getEffectiveFrom())
            .effectiveTo(template.getEffectiveTo())
            .isCurrentlyEffective(template.isCurrentlyEffective())
            .notes(template.getNotes())
            .build();
    }
}
