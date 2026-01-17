package com.example.Fuba_BE.service.Scheduling;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Fuba_BE.domain.entity.Driver;
import com.example.Fuba_BE.domain.entity.DriverRouteAssignment;
import com.example.Fuba_BE.domain.entity.Route;
import com.example.Fuba_BE.domain.entity.Trip;
import com.example.Fuba_BE.domain.entity.TripGenerationLog;
import com.example.Fuba_BE.domain.entity.TripTemplate;
import com.example.Fuba_BE.domain.entity.Vehicle;
import com.example.Fuba_BE.domain.entity.VehicleRouteAssignment;
import com.example.Fuba_BE.dto.scheduling.TripGenerationRequest;
import com.example.Fuba_BE.dto.scheduling.TripGenerationResponse;
import com.example.Fuba_BE.exception.BadRequestException;
import com.example.Fuba_BE.exception.NotFoundException;
import com.example.Fuba_BE.repository.DriverRepository;
import com.example.Fuba_BE.repository.DriverRouteAssignmentRepository;
import com.example.Fuba_BE.repository.DriverWorkLogRepository;
import com.example.Fuba_BE.repository.RouteRepository;
import com.example.Fuba_BE.repository.TripGenerationLogRepository;
import com.example.Fuba_BE.repository.TripRepository;
import com.example.Fuba_BE.repository.TripTemplateRepository;
import com.example.Fuba_BE.repository.VehicleRepository;
import com.example.Fuba_BE.repository.VehicleRouteAssignmentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for automatic trip generation from templates
 * Supports: Round-trip, Interval scheduling, Day-of-week filtering, 10-hour limit validation
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TripGenerationService implements ITripGenerationService {

    private static final double MAX_WORKING_HOURS = 10.0;
    private static final int REST_TIME_MINUTES = 60; // 1 hour rest between outbound and return trip

    private final TripTemplateRepository templateRepository;
    private final TripRepository tripRepository;
    private final RouteRepository routeRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRouteAssignmentRepository driverAssignmentRepository;
    private final VehicleRouteAssignmentRepository vehicleAssignmentRepository;
    private final DriverWorkLogRepository workLogRepository;
    private final TripGenerationLogRepository generationLogRepository;

    @Override
    public TripGenerationResponse generateTripsFromTemplate(TripGenerationRequest request) {
        return executeGeneration(request, false);
    }

    @Override
    @Transactional(readOnly = true)
    public TripGenerationResponse previewTripGeneration(TripGenerationRequest request) {
        request.setDryRun(true);
        return executeGeneration(request, true);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateDriverWorkingHours(Integer driverId, LocalDate date, double additionalHours) {
        double currentHours = calculateDriverHoursOnDate(driverId, date);
        return (currentHours + additionalHours) <= MAX_WORKING_HOURS;
    }

    @Override
    @Transactional(readOnly = true)
    public double calculateDriverHoursOnDate(Integer driverId, LocalDate date) {
        // Get all trips for driver on this date
        List<Trip> trips = tripRepository.findTripsByDriverAndDate(driverId, date);
        
        double totalHours = 0.0;
        for (Trip trip : trips) {
            Duration duration = Duration.between(trip.getDepartureTime(), trip.getArrivalTime());
            totalHours += duration.toMinutes() / 60.0;
        }
        
        return totalHours;
    }

    // ========== MAIN GENERATION LOGIC ==========

    private TripGenerationResponse executeGeneration(TripGenerationRequest request, boolean isPreview) {
        log.info("Starting trip generation. Template: {}, Period: {} to {}, DryRun: {}", 
                 request.getTemplateId(), request.getStartDate(), request.getEndDate(), isPreview);

        // 1. Validate and load template
        TripTemplate template = validateAndLoadTemplate(request);

        // 2. Initialize response
        TripGenerationResponse response = initializeResponse(request, template);
        TripGenerationResponse.GenerationBreakdown breakdown = TripGenerationResponse.GenerationBreakdown.builder().build();

        // 3. Get route details
        Route route = template.getRoute();
        int durationMinutes = route.getEstimatedDuration();

        // 4. Generate trips day by day
        List<Trip> createdTrips = new ArrayList<>();
        LocalDate currentDate = request.getStartDate();
        int totalDays = 0;
        int applicableDays = 0;

        while (!currentDate.isAfter(request.getEndDate())) {
            totalDays++;

            // Check if template applies to this date
            if (!template.appliesToDate(currentDate)) {
                response.getSkipReasons().add(String.format("[%s] Skipped: Not in allowed days (%s)", 
                                                             currentDate, template.getDaysOfWeek()));
                currentDate = currentDate.plusDays(1);
                continue;
            }

            applicableDays++;

            // Get departure times for this day (based on interval)
            List<LocalTime> departureTimes = template.getDepartureTimesForDay();

            for (LocalTime departureTime : departureTimes) {
                // ========== OUTBOUND TRIP ==========
                TripGenerationContext context = TripGenerationContext.builder()
                    .template(template)
                    .route(route)
                    .date(currentDate)
                    .departureTime(departureTime)
                    .durationMinutes(durationMinutes)
                    .isReturnTrip(false)
                    .request(request)
                    .breakdown(breakdown)
                    .build();

                Trip outboundTrip = generateSingleTrip(context, response);

                if (outboundTrip != null) {
                    if (!isPreview) {
                        createdTrips.add(outboundTrip);
                    }
                    breakdown.setOutboundTrips(breakdown.getOutboundTrips() + 1);

                    // ========== RETURN TRIP (if enabled) ==========
                    if (template.shouldGenerateRoundTrip()) {
                        Trip returnTrip = generateReturnTrip(outboundTrip, template, request, breakdown, response);
                        if (returnTrip != null) {
                            if (!isPreview) {
                                createdTrips.add(returnTrip);
                            }
                            breakdown.setReturnTrips(breakdown.getReturnTrips() + 1);
                        }
                    }
                }
            }

            currentDate = currentDate.plusDays(1);
        }

        // 5. Save trips if not dry-run
        if (!isPreview && !createdTrips.isEmpty()) {
            tripRepository.saveAll(createdTrips);
            log.info("Saved {} trips to database", createdTrips.size());

            // Save generation log
            saveGenerationLog(template, request, createdTrips.size(), response.getSkipReasons());
        }

        // 6. Finalize response
        response.setTotalDaysRequested(totalDays);
        response.setApplicableDays(applicableDays);
        response.setCreatedTrips(createdTrips.size());
        response.setSkippedCount(response.getSkipReasons().size());
        response.setBreakdown(breakdown);
        response.setStatus(determineStatus(createdTrips.size(), response.getSkipReasons().size()));
        response.setMessage(generateSummaryMessage(response));

        log.info("Generation completed. Created: {}, Skipped: {}, Status: {}", 
                 response.getCreatedTrips(), response.getSkippedCount(), response.getStatus());

        return response;
    }

    // ========== SINGLE TRIP GENERATION ==========

    private Trip generateSingleTrip(TripGenerationContext context, TripGenerationResponse response) {
        LocalDateTime departureDateTime = LocalDateTime.of(context.getDate(), context.getDepartureTime());
        LocalDateTime arrivalDateTime = departureDateTime.plusMinutes(context.getDurationMinutes());

        Route effectiveRoute = context.isReturnTrip() ? 
            getOrCreateReverseRoute(context.getRoute()) : context.getRoute();

        // Check 1: Trip already exists
        if (context.getRequest().getSkipExistingTrips() && 
            tripRepository.existsByRouteAndDepartureTime(effectiveRoute.getRouteId(), departureDateTime)) {
            
            response.getSkipReasons().add(String.format("[%s %s] Already exists", 
                                                         context.getDate(), context.getDepartureTime()));
            context.getBreakdown().setSkippedAlreadyExists(context.getBreakdown().getSkippedAlreadyExists() + 1);
            return null;
        }

        // Check 2: Find available driver
        Driver driver = null;
        if (context.getTemplate().getAutoAssignDriver() && context.getRequest().getAutoAssignDrivers()) {
            driver = findAvailableDriver(effectiveRoute, context.getDate(), departureDateTime, arrivalDateTime, 
                                         context.getRequest(), context.getBreakdown(), response);
            if (driver == null) {
                response.getSkipReasons().add(String.format("[%s %s] No available driver", 
                                                             context.getDate(), context.getDepartureTime()));
                context.getBreakdown().setSkippedNoDriver(context.getBreakdown().getSkippedNoDriver() + 1);
                if (!context.getRequest().getAllowPartialGeneration()) {
                    return null;
                }
            }
        }

        // Check 3: Find available vehicle
        Vehicle vehicle = null;
        if (context.getTemplate().getAutoAssignVehicle() && context.getRequest().getAutoAssignVehicles()) {
            vehicle = findAvailableVehicle(effectiveRoute, context.getDate(), departureDateTime, arrivalDateTime, 
                                           context.getBreakdown(), response);
            if (vehicle == null) {
                response.getSkipReasons().add(String.format("[%s %s] No available vehicle", 
                                                             context.getDate(), context.getDepartureTime()));
                context.getBreakdown().setSkippedNoVehicle(context.getBreakdown().getSkippedNoVehicle() + 1);
                if (!context.getRequest().getAllowPartialGeneration()) {
                    return null;
                }
            }
        }

        // Create trip entity
        Trip trip = new Trip();
        trip.setRoute(effectiveRoute);
        trip.setDriver(driver);
        trip.setVehicle(vehicle);
        trip.setDepartureTime(departureDateTime);
        trip.setArrivalTime(arrivalDateTime);
        trip.setBasePrice(context.getTemplate().getBasePrice());
        trip.setStatus("Scheduled");

        return trip;
    }

    // ========== RETURN TRIP GENERATION ==========

    private Trip generateReturnTrip(Trip outboundTrip, TripTemplate template, TripGenerationRequest request,
                                   TripGenerationResponse.GenerationBreakdown breakdown, TripGenerationResponse response) {
        
        // Return trip departs after outbound arrives + rest time
        LocalDateTime returnDeparture = outboundTrip.getArrivalTime().plusMinutes(REST_TIME_MINUTES);
        LocalDate returnDate = returnDeparture.toLocalDate();
        LocalTime returnTime = returnDeparture.toLocalTime();

        // Create reverse route
        Route reverseRoute = getOrCreateReverseRoute(outboundTrip.getRoute());
        int durationMinutes = reverseRoute.getEstimatedDuration();

        TripGenerationContext returnContext = TripGenerationContext.builder()
            .template(template)
            .route(reverseRoute)
            .date(returnDate)
            .departureTime(returnTime)
            .durationMinutes(durationMinutes)
            .isReturnTrip(true)
            .request(request)
            .breakdown(breakdown)
            .build();

        return generateSingleTrip(returnContext, response);
    }

    // ========== REVERSE ROUTE HANDLING ==========

    private Route getOrCreateReverseRoute(Route originalRoute) {
        // Try to find existing reverse route
        Optional<Route> existingReverse = routeRepository.findByOriginAndDestination(
            originalRoute.getDestination().getLocationId(),
            originalRoute.getOrigin().getLocationId()
        );

        if (existingReverse.isPresent()) {
            return existingReverse.get();
        }

        // Create new reverse route
        log.info("Creating reverse route: {} → {}", 
                 originalRoute.getDestination().getLocationName(), 
                 originalRoute.getOrigin().getLocationName());

        Route reverseRoute = new Route();
        reverseRoute.setOrigin(originalRoute.getDestination());
        reverseRoute.setDestination(originalRoute.getOrigin());
        reverseRoute.setRouteName(originalRoute.getDestination().getLocationName() + " - " + 
                                  originalRoute.getOrigin().getLocationName());
        reverseRoute.setDistance(originalRoute.getDistance());
        reverseRoute.setEstimatedDuration(originalRoute.getEstimatedDuration());
        reverseRoute.setStatus("Active");

        return routeRepository.save(reverseRoute);
    }

    // ========== DRIVER SELECTION ==========

    private Driver findAvailableDriver(Route route, LocalDate date, LocalDateTime departureTime, 
                                      LocalDateTime arrivalTime, TripGenerationRequest request,
                                      TripGenerationResponse.GenerationBreakdown breakdown,
                                      TripGenerationResponse response) {
        
        // Get drivers assigned to this route (ordered by priority)
        List<DriverRouteAssignment> assignments = driverAssignmentRepository.findEffectiveByRouteAndDate(
            route.getRouteId(), date
        );

        if (assignments.isEmpty()) {
            log.debug("No driver assignments found for route {}", route.getRouteId());
            return null;
        }

        double tripDurationHours = Duration.between(departureTime, arrivalTime).toMinutes() / 60.0;

        for (DriverRouteAssignment assignment : assignments) {
            Driver driver = assignment.getDriver();

            // Check 10-hour limit
            if (request.getRespectWorkingHourLimit()) {
                if (!validateDriverWorkingHours(driver.getDriverId(), date, tripDurationHours)) {
                    log.debug("Driver {} would exceed 10-hour limit", driver.getDriverId());
                    breakdown.setSkippedDriverOverLimit(breakdown.getSkippedDriverOverLimit() + 1);
                    continue;
                }
            }

            // Check time conflict
            if (tripRepository.existsByDriverAndTimeOverlap(driver.getDriverId(), departureTime, arrivalTime)) {
                log.debug("Driver {} has time conflict", driver.getDriverId());
                continue;
            }

            // Driver is available!
            log.debug("Assigned driver {} (priority {}) to trip", driver.getDriverId(), assignment.getPriority());
            return driver;
        }

        return null; // No available driver found
    }

    // ========== VEHICLE SELECTION ==========

    private Vehicle findAvailableVehicle(Route route, LocalDate date, LocalDateTime departureTime, 
                                        LocalDateTime arrivalTime,
                                        TripGenerationResponse.GenerationBreakdown breakdown,
                                        TripGenerationResponse response) {
        
        // Get vehicles assigned to this route (ordered by priority)
        List<VehicleRouteAssignment> assignments = vehicleAssignmentRepository.findEffectiveByRouteAndDate(
            route.getRouteId(), date
        );

        if (assignments.isEmpty()) {
            log.debug("No vehicle assignments found for route {}", route.getRouteId());
            return null;
        }

        for (VehicleRouteAssignment assignment : assignments) {
            Vehicle vehicle = assignment.getVehicle();

            // Check maintenance
            if (assignment.needsMaintenance()) {
                log.debug("Vehicle {} needs maintenance", vehicle.getVehicleId());
                continue;
            }

            // Check time conflict
            if (tripRepository.existsByVehicleAndTimeOverlap(vehicle.getVehicleId(), departureTime, arrivalTime)) {
                log.debug("Vehicle {} has time conflict", vehicle.getVehicleId());
                continue;
            }

            // Vehicle is available!
            log.debug("Assigned vehicle {} (priority {}) to trip", vehicle.getVehicleId(), assignment.getPriority());
            return vehicle;
        }

        return null; // No available vehicle found
    }

    // ========== VALIDATION & HELPERS ==========

    private TripTemplate validateAndLoadTemplate(TripGenerationRequest request) {
        TripTemplate template = templateRepository.findById(request.getTemplateId())
            .orElseThrow(() -> new NotFoundException("Template not found with ID: " + request.getTemplateId()));

        if (!template.getIsActive()) {
            throw new BadRequestException("Template is not active");
        }

        if (!template.isValidGenerationPeriod(request.getStartDate(), request.getEndDate())) {
            throw new BadRequestException(String.format(
                "Generation period exceeds maximum %d days allowed by template", 
                template.getMaxGenerationDays()
            ));
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date must be after start date");
        }

        return template;
    }

    private TripGenerationResponse initializeResponse(TripGenerationRequest request, TripTemplate template) {
        return TripGenerationResponse.builder()
            .templateId(template.getTemplateId())
            .templateName(template.getTemplateName())
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .isDryRun(request.getDryRun())
            .skipReasons(new ArrayList<>())
            .build();
    }

    private String determineStatus(int created, int skipped) {
        if (created == 0) return "Failed";
        if (skipped > 0) return "PartialSuccess";
        return "Success";
    }

    private String generateSummaryMessage(TripGenerationResponse response) {
        if (response.getIsDryRun()) {
            return String.format("Preview: Would create %d trips, skip %d", 
                               response.getCreatedTrips(), response.getSkippedCount());
        }
        return String.format("Successfully created %d trips, skipped %d", 
                           response.getCreatedTrips(), response.getSkippedCount());
    }

    private void saveGenerationLog(TripTemplate template, TripGenerationRequest request, 
                                   int createdCount, List<String> skipReasons) {
        TripGenerationLog log = TripGenerationLog.builder()
            .template(template)
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .totalTripsCreated(createdCount)
            .totalTripsSkipped(skipReasons.size())
            .status("Success")
            .skipReasons(String.join("\n", skipReasons))
            .build();
        
        generationLogRepository.save(log);
    }

    // ========== CONTEXT HOLDER ==========

    @lombok.Data
    @lombok.Builder
    private static class TripGenerationContext {
        private TripTemplate template;
        private Route route;
        private LocalDate date;
        private LocalTime departureTime;
        private int durationMinutes;
        private boolean isReturnTrip;
        private TripGenerationRequest request;
        private TripGenerationResponse.GenerationBreakdown breakdown;
    }
}
