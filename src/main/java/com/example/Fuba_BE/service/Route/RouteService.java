package com.example.Fuba_BE.service.Route;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.Fuba_BE.domain.entity.Location;
import com.example.Fuba_BE.domain.entity.Route;
import com.example.Fuba_BE.domain.entity.RouteStop;
import com.example.Fuba_BE.dto.Routes.RouteRequestDTO;
import com.example.Fuba_BE.dto.Routes.RouteResponseDTO;
import com.example.Fuba_BE.dto.Routes.RouteSelectionDTO;
import com.example.Fuba_BE.dto.Routes.RouteStopResponseDTO;
import com.example.Fuba_BE.exception.BadRequestException;
import com.example.Fuba_BE.exception.ResourceNotFoundException;
import com.example.Fuba_BE.mapper.RouteMapper;
import com.example.Fuba_BE.mapper.SelectionMapper;
import com.example.Fuba_BE.repository.LocationRepository;
import com.example.Fuba_BE.repository.RouteRepository;
import com.example.Fuba_BE.repository.RouteStopRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RouteService implements IRouteService {

    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final LocationRepository locationRepository;
    private final RouteMapper routeMapper;
    private final SelectionMapper selectionMapper;

    // Use SLF4J standard from Spring Boot
    private static final Logger log = LoggerFactory.getLogger(RouteService.class);

    // --- Helper: Find Location by name ---
    private Location getLocationByName(String locationName) {
        return locationRepository.findByLocationName(locationName)
                .orElseThrow(() -> {
                    log.error("❌ Error: Location not found with name '{}'", locationName);
                    return new ResourceNotFoundException("Location not found: " + locationName);
                });
    }

    @Override
    public RouteResponseDTO createRoute(RouteRequestDTO request) {
        log.info("🚀 Creating new route: {} -> {}", request.getOriginName(), request.getDestinationName());
        try {
            // Validate all locations exist BEFORE starting transaction
            validateAllLocationsExist(request);
            // Validate business rules
            validateRouteRequest(request);
            // Check for duplicate route
            checkDuplicateRoute(request);
            
            Route route = routeMapper.toEntity(request);

            Location origin = getLocationByName(request.getOriginName());
            Location destination = getLocationByName(request.getDestinationName());

            route.setOrigin(origin);
            route.setDestination(destination);
            if (route.getStatus() == null) route.setStatus("Active");

            Route savedRoute = routeRepository.save(route);
            log.info("✅ Route entity saved with ID: {}", savedRoute.getRouteId());

            createRouteStops(savedRoute, origin, destination, request.getIntermediateStopNames());
            log.info("✅ Route stops created for Route ID: {}", savedRoute.getRouteId());

            return enrichSingleRoute(savedRoute);

        } catch (BadRequestException | ResourceNotFoundException e) {
            // Re-throw known exceptions
            throw e;
        } catch (Exception e) {
            log.error("🔥 Critical error creating route: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to create route: " + e.getMessage());
        }
    }

    @Override
    public RouteResponseDTO updateRoute(Integer routeId, RouteRequestDTO request) {
        log.info("🔄 Updating route ID: {}", routeId);
        try {
            // Validate all locations exist BEFORE starting transaction
            validateAllLocationsExist(request);
            // Validate business rules
            validateRouteRequest(request);
            
            Route route = routeRepository.findById(routeId)
                    .orElseThrow(() -> {
                        log.error("❌ Route not found for update: ID {}", routeId);
                        return new ResourceNotFoundException("Route not found with ID: " + routeId);
                    });

            routeMapper.updateEntityFromDto(request, route);
            Location origin = getLocationByName(request.getOriginName());
            Location destination = getLocationByName(request.getDestinationName());
            route.setOrigin(origin);
            route.setDestination(destination);

            Route updatedRoute = routeRepository.save(route);
            log.info("✅ Route basic info updated for ID: {}", routeId);

            // Delete and recreate route stops
            log.info("🧹 Deleting old route stops for Route ID: {}", routeId);
            routeStopRepository.deleteAll(routeStopRepository.findByRoute_RouteIdOrderByStopOrderAsc(routeId));
            routeStopRepository.flush();

            log.info("🛠 Creating new route stops...");
            createRouteStops(updatedRoute, origin, destination, request.getIntermediateStopNames());

            return enrichSingleRoute(updatedRoute);

        } catch (BadRequestException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("🔥 Critical error updating route ID {}: {}", routeId, e.getMessage(), e);
            throw new BadRequestException("Failed to update route: " + e.getMessage());
        }
    }

    @Override
    public void deleteRoute(Integer routeId) {
        log.info("🗑 Deleting route ID: {}", routeId);
        try {
            if (!routeRepository.existsById(routeId)) {
                log.error("❌ Cannot delete - route not found: ID {}", routeId);
                throw new ResourceNotFoundException("Route not found with ID: " + routeId);
            }
            routeRepository.deleteById(routeId);
            log.info("✅ Route deleted successfully: ID {}", routeId);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("🔥 Error deleting route ID {}: {}", routeId, e.getMessage(), e);
            throw new BadRequestException("Failed to delete route: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RouteResponseDTO> getAllRoutesForUI(Pageable pageable) {
        // Log mức debug để tránh spam log nếu gọi nhiều
        log.debug("🔍 Lấy danh sách Routes (Pagination)...");
        Page<Route> page = routeRepository.findAll(pageable);
        return enrichRoutePage(page);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RouteResponseDTO> searchRoutes(String keyword, Pageable pageable) {
        log.info("🔎 Searching routes with keyword: '{}'", keyword);
        Page<Route> page;
        if (!StringUtils.hasText(keyword)) {
            page = routeRepository.findAll(pageable);
        } else {
            page = routeRepository.searchRoutes(keyword.trim(), pageable);
        }
        return enrichRoutePage(page);
    }

    private Page<RouteResponseDTO> enrichRoutePage(Page<Route> page) {
        if (page.isEmpty()) return page.map(routeMapper::toResponseDTO);

        List<Integer> routeIds = page.getContent().stream()
                .map(Route::getRouteId)
                .toList();

        List<RouteStop> allStops = routeStopRepository.findByRouteIdIn(routeIds);

        Map<Integer, List<RouteStop>> stopsMap = allStops.stream()
                .collect(Collectors.groupingBy(s -> s.getRoute().getRouteId()));

        return page.map(route -> {
            RouteResponseDTO dto = routeMapper.toResponseDTO(route);
            List<RouteStop> myStops = stopsMap.getOrDefault(route.getRouteId(), Collections.emptyList());
            dto.setStopNames(myStops.stream()
                    .map(s -> s.getLocation().getLocationName())
                    .toList());
            dto.setTotalStops(myStops.size());
            return dto;
        });
    }

    private RouteResponseDTO enrichSingleRoute(Route route) {
        RouteResponseDTO dto = routeMapper.toResponseDTO(route);
        List<RouteStop> stops = routeStopRepository.findByRoute_RouteIdOrderByStopOrderAsc(route.getRouteId());

        dto.setStopNames(stops.stream().map(s -> s.getLocation().getLocationName()).toList());
        dto.setTotalStops(stops.size());
        return dto;
    }

    @Override
    public List<RouteStopResponseDTO> getAllRouteStop() {
        return routeStopRepository.findAllRouteStopBasic();
    }

    private void validateRouteRequest(RouteRequestDTO request) {
        if (request.getOriginName() == null || request.getDestinationName() == null) {
            log.warn("⚠️ Validation failed: Origin or destination is null");
            throw new BadRequestException("Origin and destination cannot be empty");
        }
        if (request.getOriginName().equalsIgnoreCase(request.getDestinationName())) {
            log.warn("⚠️ Validation failed: Origin equals destination ({})", request.getOriginName());
            throw new BadRequestException("Origin and destination must be different");
        }
    }

    /**
     * Validate all locations exist BEFORE starting transaction
     * This prevents partial rollback and provides better error messages
     */
    private void validateAllLocationsExist(RouteRequestDTO request) {
        log.debug("Validating all locations exist...");
        
        // Check origin
        locationRepository.findByLocationName(request.getOriginName())
                .orElseThrow(() -> {
                    log.error("❌ Origin location not found: '{}'", request.getOriginName());
                    return new ResourceNotFoundException("Origin location not found: " + request.getOriginName());
                });
        
        // Check destination
        locationRepository.findByLocationName(request.getDestinationName())
                .orElseThrow(() -> {
                    log.error("❌ Destination location not found: '{}'", request.getDestinationName());
                    return new ResourceNotFoundException("Destination location not found: " + request.getDestinationName());
                });
        
        // Check all intermediate stops
        if (request.getIntermediateStopNames() != null && !request.getIntermediateStopNames().isEmpty()) {
            for (String stopName : request.getIntermediateStopNames()) {
                String normalized = stopName.trim();
                if (!normalized.isEmpty()) {
                    locationRepository.findByLocationName(normalized)
                            .orElseThrow(() -> {
                                log.error("❌ Intermediate stop location not found: '{}'", normalized);
                                return new ResourceNotFoundException("Intermediate stop location not found: " + normalized);
                            });
                }
            }
        }
        
        log.debug("✅ All locations validated successfully");
    }

    /**
     * Check if a route with the same origin and destination already exists
     */
    private void checkDuplicateRoute(RouteRequestDTO request) {
        Location origin = locationRepository.findByLocationName(request.getOriginName()).get();
        Location destination = locationRepository.findByLocationName(request.getDestinationName()).get();
        
        boolean exists = routeRepository.existsByOriginAndDestination(origin, destination);
        if (exists) {
            log.warn("⚠️ Duplicate route detected: {} -> {}", request.getOriginName(), request.getDestinationName());
            throw new BadRequestException("Route from " + request.getOriginName() + " to " + request.getDestinationName() + " already exists");
        }
    }

    private void createRouteStops(Route route, Location origin, Location destination, List<String> intermediateStopNames) {
        log.debug("Creating stops for Route ID: {}", route.getRouteId());
        int orderCounter = 1;

        // 1. Start Stop
        RouteStop startStop = createStop(route, origin, orderCounter++, "ORIGIN", true, false, BigDecimal.ZERO, 0);
        routeStopRepository.save(startStop);
        log.debug("   + Origin stop created: {}", origin.getLocationName());

        // 2. Intermediate Stops
        if (intermediateStopNames != null) {
            for (String stopName : intermediateStopNames) {
                String normalized = stopName.trim();
                if (normalized.equalsIgnoreCase(origin.getLocationName()) ||
                        normalized.equalsIgnoreCase(destination.getLocationName())) {
                    log.warn("   ⚠️ Skipping duplicate stop: {}", stopName);
                    continue;
                }

                Location loc = getLocationByName(stopName);
                RouteStop stop = createStop(route, loc, orderCounter++, "INTERMEDIATE", true, true, BigDecimal.ZERO, 0);
                routeStopRepository.save(stop);
                log.debug("   + Intermediate stop created: {}", stopName);
            }
        }

        // 3. End Stop
        RouteStop endStop = createStop(route, destination, orderCounter, "DESTINATION", false, true, route.getDistance(), route.getEstimatedDuration());
        routeStopRepository.save(endStop);
        log.debug("   + Destination stop created: {}", destination.getLocationName());
    }

    private RouteStop createStop(Route route, Location loc, int order, String type, boolean pickup, boolean dropoff, BigDecimal dist, int time) {
        RouteStop s = new RouteStop();
        s.setRoute(route); s.setLocation(loc); s.setStopOrder(order); s.setStopType(type);
        s.setStopName(loc.getLocationName()); s.setStopAddress(loc.getAddress());
        s.setLatitude(loc.getLatitude()); s.setLongitude(loc.getLongitude());
        s.setIsPickupPoint(pickup); s.setIsDropoffPoint(dropoff);
        s.setDistanceFromOrigin(dist); s.setEstimatedTime(time);
        return s;
    }

    @Override
    public List<RouteSelectionDTO> getAllRoutesForSelection() {
        return routeRepository.findByStatus("Active").stream()
                .map(selectionMapper::toRouteSelectionDTO)
                .toList();
    }
}