package com.example.Fuba_BE.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.Fuba_BE.domain.entity.Driver;
import com.example.Fuba_BE.domain.entity.DriverRouteAssignment;
import com.example.Fuba_BE.domain.entity.Route;
import com.example.Fuba_BE.domain.entity.Trip;
import com.example.Fuba_BE.domain.entity.TripSeat;
import com.example.Fuba_BE.domain.entity.Vehicle;
import com.example.Fuba_BE.domain.entity.VehicleRouteAssignment;
import com.example.Fuba_BE.domain.entity.VehicleType;
import com.example.Fuba_BE.exception.BadRequestException;
import com.example.Fuba_BE.exception.NotFoundException;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.repository.DriverRouteAssignmentRepository;
import com.example.Fuba_BE.repository.RouteRepository;
import com.example.Fuba_BE.repository.TripRepository;
import com.example.Fuba_BE.repository.TripSeatRepository;
import com.example.Fuba_BE.repository.VehicleRepository;
import com.example.Fuba_BE.repository.VehicleRouteAssignmentRepository;
import com.example.Fuba_BE.repository.VehicleTypeRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for bulk seeding vehicles and trips
 * Supports creating multiple vehicles with auto-assignment to routes
 * and generating trips with round-trip logic (outbound + return)
 */
@RestController
@RequestMapping("/admin/seed")
@RequiredArgsConstructor
@Slf4j
public class BulkSeederController {

    private final VehicleRepository vehicleRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final RouteRepository routeRepository;
    private final VehicleRouteAssignmentRepository vehicleAssignmentRepository;
    private final DriverRouteAssignmentRepository driverAssignmentRepository;
    private final TripRepository tripRepository;
    private final TripSeatRepository tripSeatRepository;

    // =====================================================================
    // BULK VEHICLE SEEDING
    // =====================================================================

    /**
     * Bulk create vehicles with auto-assignment to routes
     * POST /admin/seed/vehicles
     */
    @PostMapping("/vehicles")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> seedVehicles(
            @Valid @RequestBody BulkVehicleRequest request) {

        log.info("🚌 Starting bulk vehicle seeding: {} vehicles", request.getCount());

        // Validate vehicle type
        VehicleType vehicleType = vehicleTypeRepository.findById(request.getVehicleTypeId())
                .orElseThrow(() -> new NotFoundException("VehicleType not found: " + request.getVehicleTypeId()));

        // Get routes for assignment
        List<Route> routes = routeRepository.findAllActiveWithLocations();
        if (routes.isEmpty()) {
            throw new BadRequestException("No active routes found");
        }

        // Limit to requested route count
        int maxRoutes = Math.min(routes.size(), request.getAssignToRouteCount());
        List<Route> selectedRoutes = routes.subList(0, maxRoutes);

        List<Vehicle> createdVehicles = new ArrayList<>();
        List<VehicleRouteAssignment> createdAssignments = new ArrayList<>();

        String licensePlatePrefix = request.getLicensePlatePrefix() != null ? request.getLicensePlatePrefix() : "51B-";

        for (int i = 1; i <= request.getCount(); i++) {
            String licensePlate = String.format("%s%05d", licensePlatePrefix, 10000 + i);

            // Skip if license plate exists
            if (vehicleRepository.existsByLicensePlate(licensePlate)) {
                log.warn("⚠️ Skipping - License plate exists: {}", licensePlate);
                continue;
            }

            Vehicle vehicle = new Vehicle();
            vehicle.setLicensePlate(licensePlate);
            vehicle.setVehicleType(vehicleType);
            vehicle.setStatus("Operational");
            vehicle.setInsuranceNumber("INS-" + licensePlate);
            vehicle.setInsuranceExpiry(LocalDate.of(2030, 12, 31));

            vehicle = vehicleRepository.save(vehicle);
            createdVehicles.add(vehicle);

            // Assign to route (round-robin)
            Route assignedRoute = selectedRoutes.get((i - 1) % selectedRoutes.size());

            VehicleRouteAssignment assignment = VehicleRouteAssignment.builder()
                    .vehicle(vehicle)
                    .route(assignedRoute)
                    .priority((i - 1) / selectedRoutes.size() + 1)
                    .startDate(LocalDate.now())
                    .isActive(true)
                    .build();

            assignment = vehicleAssignmentRepository.save(assignment);
            createdAssignments.add(assignment);

            if (i % 10 == 0) {
                log.info("📊 Progress: {}/{} vehicles created", i, request.getCount());
            }
        }

        log.info("✅ Bulk vehicle seeding completed!");

        Map<String, Object> result = new HashMap<>();
        result.put("vehiclesCreated", createdVehicles.size());
        result.put("assignmentsCreated", createdAssignments.size());
        result.put("routesUsed", selectedRoutes.size());
        result.put("vehicleType", vehicleType.getTypeName());

        return ResponseEntity.ok(ApiResponse.success("Vehicles created successfully", result));
    }

    // =====================================================================
    // BULK TRIP GENERATION WITH ROUND-TRIP
    // =====================================================================

    /**
     * Bulk create trips for a route with round-trip support
     * 1 driver + 1 vehicle = 2 trips (outbound + return)
     * POST /admin/seed/trips
     */
    @PostMapping("/trips")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> seedTrips(
            @Valid @RequestBody BulkTripRequest request) {

        log.info("🚀 Starting bulk trip generation for route: {}", request.getRouteId());

        // Validate route with locations
        Route outboundRoute = routeRepository.findByIdWithLocations(request.getRouteId())
                .orElseThrow(() -> new NotFoundException("Route not found: " + request.getRouteId()));

        // Get or create reverse route
        Route returnRoute = getOrCreateReverseRoute(outboundRoute);

        // Get available drivers for this route
        List<DriverRouteAssignment> driverAssignments = driverAssignmentRepository
                .findEffectiveByRouteAndDate(request.getRouteId(), request.getTripDate());

        if (driverAssignments.isEmpty()) {
            throw new BadRequestException("No drivers assigned to route " + request.getRouteId());
        }

        // Get available vehicles for this route
        List<VehicleRouteAssignment> vehicleAssignments = vehicleAssignmentRepository
                .findEffectiveByRouteAndDate(request.getRouteId(), request.getTripDate());

        if (vehicleAssignments.isEmpty()) {
            throw new BadRequestException("No vehicles assigned to route " + request.getRouteId());
        }

        int tripDurationMinutes = outboundRoute.getEstimatedDuration();
        int restTimeMinutes = request.getRestTimeMinutes() != null ? request.getRestTimeMinutes() : 60;

        List<Trip> createdTrips = new ArrayList<>();
        List<String> skipReasons = new ArrayList<>();

        LocalTime currentDepartureTime = request.getFirstDepartureTime();
        int pairsCreated = 0;
        int maxPairs = Math.min(driverAssignments.size(), vehicleAssignments.size());
        maxPairs = Math.min(maxPairs, request.getMaxTripPairs() != null ? request.getMaxTripPairs() : 10);

        for (int i = 0; i < maxPairs; i++) {
            Driver driver = driverAssignments.get(i).getDriver();
            Vehicle vehicle = vehicleAssignments.get(i).getVehicle();

            // Calculate trip times
            LocalDateTime outboundDeparture = LocalDateTime.of(request.getTripDate(), currentDepartureTime);
            LocalDateTime outboundArrival = outboundDeparture.plusMinutes(tripDurationMinutes);

            // Check driver availability for outbound
            if (isDriverBusy(driver.getDriverId(), outboundDeparture, outboundArrival)) {
                skipReasons.add(String.format("Driver %d busy at %s", driver.getDriverId(), outboundDeparture));
                continue;
            }

            // Check vehicle availability for outbound
            if (isVehicleBusy(vehicle.getVehicleId(), outboundDeparture, outboundArrival)) {
                skipReasons.add(String.format("Vehicle %d busy at %s", vehicle.getVehicleId(), outboundDeparture));
                continue;
            }

            // Create OUTBOUND trip
            Trip outboundTrip = createTrip(outboundRoute, driver, vehicle, outboundDeparture,
                    outboundArrival, request.getBasePrice());
            outboundTrip = tripRepository.save(outboundTrip);
            createTripSeats(outboundTrip, vehicle);
            createdTrips.add(outboundTrip);

            // Calculate RETURN trip times
            LocalDateTime returnDeparture = outboundArrival.plusMinutes(restTimeMinutes);
            LocalDateTime returnArrival = returnDeparture.plusMinutes(tripDurationMinutes);

            // Check driver availability for return
            if (isDriverBusy(driver.getDriverId(), returnDeparture, returnArrival)) {
                skipReasons
                        .add(String.format("Driver %d busy for return at %s", driver.getDriverId(), returnDeparture));
                // Still count the outbound
                pairsCreated++;
                currentDepartureTime = currentDepartureTime.plusMinutes(request.getIntervalMinutes());
                continue;
            }

            // Check vehicle availability for return
            if (isVehicleBusy(vehicle.getVehicleId(), returnDeparture, returnArrival)) {
                skipReasons.add(
                        String.format("Vehicle %d busy for return at %s", vehicle.getVehicleId(), returnDeparture));
                pairsCreated++;
                currentDepartureTime = currentDepartureTime.plusMinutes(request.getIntervalMinutes());
                continue;
            }

            // Create RETURN trip
            Trip returnTrip = createTrip(returnRoute, driver, vehicle, returnDeparture,
                    returnArrival, request.getBasePrice());
            returnTrip = tripRepository.save(returnTrip);
            createTripSeats(returnTrip, vehicle);
            createdTrips.add(returnTrip);

            pairsCreated++;

            // Move to next time slot
            currentDepartureTime = currentDepartureTime.plusMinutes(request.getIntervalMinutes());

            log.info("✅ Created trip pair {} for driver {} vehicle {}",
                    pairsCreated, driver.getDriverId(), vehicle.getLicensePlate());
        }

        log.info("✅ Bulk trip generation completed! Created {} trips ({} pairs)",
                createdTrips.size(), pairsCreated);

        Map<String, Object> result = new HashMap<>();
        result.put("tripsCreated", createdTrips.size());
        result.put("pairsCreated", pairsCreated);
        result.put("outboundRoute", outboundRoute.getRouteName());
        result.put("returnRoute", returnRoute.getRouteName());
        result.put("tripDate", request.getTripDate().toString());
        result.put("driversUsed", Math.min(pairsCreated, driverAssignments.size()));
        result.put("vehiclesUsed", Math.min(pairsCreated, vehicleAssignments.size()));
        result.put("skipReasons", skipReasons);

        return ResponseEntity.ok(ApiResponse.success("Trips created successfully", result));
    }

    /**
     * Quick route assignment - assign available drivers and vehicles to a route
     * and create trips for today/tomorrow
     * POST /admin/seed/quick-route-setup
     */
    @PostMapping("/quick-route-setup")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> quickRouteSetup(
            @RequestParam Integer routeId,
            @RequestParam(defaultValue = "1") Integer vehicleCount,
            @RequestParam(defaultValue = "08:00") String firstDepartureTime,
            @RequestParam(defaultValue = "60") Integer intervalMinutes) {

        log.info("🚀 Quick route setup for route: {}", routeId);

        Route route = routeRepository.findByIdWithLocations(routeId)
                .orElseThrow(() -> new NotFoundException("Route not found: " + routeId));

        // Find unassigned vehicles
        List<Vehicle> allVehicles = vehicleRepository.findByStatus("Operational");
        List<Vehicle> availableVehicles = new ArrayList<>();

        for (Vehicle v : allVehicles) {
            boolean alreadyAssigned = vehicleAssignmentRepository.existsByVehicleAndRoute(v.getVehicleId(), routeId);
            if (!alreadyAssigned && availableVehicles.size() < vehicleCount) {
                availableVehicles.add(v);
            }
        }

        // Assign vehicles to route
        List<VehicleRouteAssignment> newVehicleAssignments = new ArrayList<>();
        for (int i = 0; i < availableVehicles.size(); i++) {
            Vehicle v = availableVehicles.get(i);
            VehicleRouteAssignment assignment = VehicleRouteAssignment.builder()
                    .vehicle(v)
                    .route(route)
                    .priority(i + 1)
                    .startDate(LocalDate.now())
                    .isActive(true)
                    .build();
            newVehicleAssignments.add(vehicleAssignmentRepository.save(assignment));
        }

        // Now create trips using BulkTripRequest
        BulkTripRequest tripRequest = BulkTripRequest.builder()
                .routeId(routeId)
                .tripDate(LocalDate.now().plusDays(1)) // Tomorrow
                .firstDepartureTime(LocalTime.parse(firstDepartureTime))
                .intervalMinutes(intervalMinutes)
                .basePrice(new BigDecimal("200000"))
                .maxTripPairs(vehicleCount)
                .restTimeMinutes(60)
                .build();

        // Check if we have drivers and vehicles now
        List<DriverRouteAssignment> driverAssignments = driverAssignmentRepository
                .findEffectiveByRouteAndDate(routeId, tripRequest.getTripDate());
        List<VehicleRouteAssignment> vehicleAssignments = vehicleAssignmentRepository
                .findEffectiveByRouteAndDate(routeId, tripRequest.getTripDate());

        Map<String, Object> result = new HashMap<>();
        result.put("routeId", routeId);
        result.put("routeName", route.getRouteName());
        result.put("vehiclesAssigned", newVehicleAssignments.size());
        result.put("driversAvailable", driverAssignments.size());
        result.put("vehiclesAvailable", vehicleAssignments.size());

        if (driverAssignments.isEmpty()) {
            result.put("message",
                    "Vehicles assigned, but no drivers available for this route. Please assign drivers first.");
            return ResponseEntity.ok(ApiResponse.success("Partial setup completed", result));
        }

        if (vehicleAssignments.isEmpty()) {
            result.put("message", "No vehicles available for trip creation.");
            return ResponseEntity.ok(ApiResponse.success("Partial setup completed", result));
        }

        // Generate trips
        Route returnRoute = getOrCreateReverseRoute(route);
        int tripDurationMinutes = route.getEstimatedDuration();
        List<Trip> createdTrips = new ArrayList<>();

        LocalTime currentTime = tripRequest.getFirstDepartureTime();
        int pairsToCreate = Math.min(driverAssignments.size(), vehicleAssignments.size());
        pairsToCreate = Math.min(pairsToCreate, tripRequest.getMaxTripPairs());

        for (int i = 0; i < pairsToCreate; i++) {
            Driver driver = driverAssignments.get(i).getDriver();
            Vehicle vehicle = vehicleAssignments.get(i).getVehicle();

            LocalDateTime outboundDep = LocalDateTime.of(tripRequest.getTripDate(), currentTime);
            LocalDateTime outboundArr = outboundDep.plusMinutes(tripDurationMinutes);
            LocalDateTime returnDep = outboundArr.plusMinutes(tripRequest.getRestTimeMinutes());
            LocalDateTime returnArr = returnDep.plusMinutes(tripDurationMinutes);

            // Check conflicts
            if (!isDriverBusy(driver.getDriverId(), outboundDep, outboundArr) &&
                    !isVehicleBusy(vehicle.getVehicleId(), outboundDep, outboundArr)) {

                Trip outbound = createTrip(route, driver, vehicle, outboundDep, outboundArr,
                        tripRequest.getBasePrice());
                outbound = tripRepository.save(outbound);
                createTripSeats(outbound, vehicle);
                createdTrips.add(outbound);

                if (!isDriverBusy(driver.getDriverId(), returnDep, returnArr) &&
                        !isVehicleBusy(vehicle.getVehicleId(), returnDep, returnArr)) {

                    Trip returnTrip = createTrip(returnRoute, driver, vehicle, returnDep, returnArr,
                            tripRequest.getBasePrice());
                    returnTrip = tripRepository.save(returnTrip);
                    createTripSeats(returnTrip, vehicle);
                    createdTrips.add(returnTrip);
                }
            }

            currentTime = currentTime.plusMinutes(tripRequest.getIntervalMinutes());
        }

        result.put("tripsCreated", createdTrips.size());
        result.put("tripDate", tripRequest.getTripDate().toString());

        return ResponseEntity.ok(ApiResponse.success("Quick route setup completed", result));
    }

    // =====================================================================
    // HELPER METHODS
    // =====================================================================

    private Route getOrCreateReverseRoute(Route original) {
        return routeRepository.findByOriginAndDestination(
                original.getDestination().getLocationId(),
                original.getOrigin().getLocationId()).orElseGet(() -> {
                    log.info("Creating reverse route: {} → {}",
                            original.getDestination().getLocationName(),
                            original.getOrigin().getLocationName());

                    Route reverse = new Route();
                    reverse.setOrigin(original.getDestination());
                    reverse.setDestination(original.getOrigin());
                    reverse.setRouteName(original.getDestination().getLocationName() + " - " +
                            original.getOrigin().getLocationName());
                    reverse.setDistance(original.getDistance());
                    reverse.setEstimatedDuration(original.getEstimatedDuration());
                    reverse.setStatus("Active");
                    return routeRepository.save(reverse);
                });
    }

    private boolean isDriverBusy(Integer driverId, LocalDateTime start, LocalDateTime end) {
        return tripRepository.existsByDriverAndTimeOverlap(driverId, start, end);
    }

    private boolean isVehicleBusy(Integer vehicleId, LocalDateTime start, LocalDateTime end) {
        return tripRepository.existsByVehicleAndTimeOverlap(vehicleId, start, end);
    }

    private Trip createTrip(Route route, Driver driver, Vehicle vehicle,
            LocalDateTime departure, LocalDateTime arrival, BigDecimal basePrice) {
        Trip trip = new Trip();
        trip.setRoute(route);
        trip.setDriver(driver);
        trip.setVehicle(vehicle);
        trip.setDepartureTime(departure);
        trip.setArrivalTime(arrival);
        trip.setBasePrice(basePrice);
        trip.setStatus("Scheduled");
        return trip;
    }

    private void createTripSeats(Trip trip, Vehicle vehicle) {
        VehicleType vehicleType = vehicle.getVehicleType();
        int totalSeats = vehicleType.getTotalSeats();
        int floors = vehicleType.getNumberOfFloors() != null ? vehicleType.getNumberOfFloors() : 1;
        int seatsPerFloor = (int) Math.ceil((double) totalSeats / floors);

        int seatCounter = 1;
        for (int floor = 1; floor <= floors && seatCounter <= totalSeats; floor++) {
            for (int i = 0; i < seatsPerFloor && seatCounter <= totalSeats; i++) {
                String seatNumber = String.format("%c%02d", (char) ('A' + (seatCounter - 1) / 10),
                        (seatCounter - 1) % 10 + 1);

                TripSeat tripSeat = TripSeat.builder()
                        .trip(trip)
                        .seatNumber(seatNumber)
                        .floorNumber(floor)
                        .seatType("Standard")
                        .status("Available")
                        .build();
                tripSeatRepository.save(tripSeat);
                seatCounter++;
            }
        }
    }

    // =====================================================================
    // REQUEST DTOs
    // =====================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkVehicleRequest {
        @NotNull(message = "Count is required")
        @Min(value = 1, message = "Count must be at least 1")
        private Integer count;

        @NotNull(message = "Vehicle type ID is required")
        private Integer vehicleTypeId;

        private String licensePlatePrefix;

        @Builder.Default
        private Integer assignToRouteCount = 6;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkTripRequest {
        @NotNull(message = "Route ID is required")
        private Integer routeId;

        @NotNull(message = "Trip date is required")
        private LocalDate tripDate;

        @NotNull(message = "First departure time is required")
        private LocalTime firstDepartureTime;

        @NotNull(message = "Interval minutes is required")
        @Min(value = 30, message = "Interval must be at least 30 minutes")
        private Integer intervalMinutes;

        @NotNull(message = "Base price is required")
        private BigDecimal basePrice;

        @Builder.Default
        private Integer maxTripPairs = 10;

        @Builder.Default
        private Integer restTimeMinutes = 60;
    }
}
