package com.example.Fuba_BE.service.Route;

import com.example.Fuba_BE.domain.entity.Location;
import com.example.Fuba_BE.domain.entity.Route;
import com.example.Fuba_BE.domain.entity.RouteStop;
import com.example.Fuba_BE.domain.enums.StopType;
import com.example.Fuba_BE.dto.Routes.RouteRequestDTO;
import com.example.Fuba_BE.dto.Routes.RouteResponseDTO;
import com.example.Fuba_BE.exception.BadRequestException;
import com.example.Fuba_BE.exception.ResourceNotFoundException;
import com.example.Fuba_BE.mapper.RouteMapper;
import com.example.Fuba_BE.repository.LocationRepository;
import com.example.Fuba_BE.repository.RouteRepository;
import com.example.Fuba_BE.repository.RouteStopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RouteService implements IRouteService {

    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final LocationRepository locationRepository;
    private final RouteMapper routeMapper;

    // ================= CREATE =================
    @Override
    public RouteResponseDTO createRoute(RouteRequestDTO request) {

        validateRouteRequest(request);

        Route route = routeMapper.toEntity(request);

        Location origin = getLocationById(request.getOriginId());
        Location destination = getLocationById(request.getDestinationId());

        route.setOrigin(origin);
        route.setDestination(destination);

        Route savedRoute = routeRepository.save(route);

        createRouteStops(
                savedRoute,
                origin,
                destination,
                request.getIntermediateStopIds()
        );

        return enrichRouteResponse(savedRoute);
    }

    // ================= UPDATE =================
    @Override
    public RouteResponseDTO updateRoute(Integer routeId, RouteRequestDTO request) {

        validateRouteRequest(request);

        Route route = routeRepository.findById(routeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Route not found with id: " + routeId)
                );

        routeMapper.updateEntityFromDto(request, route);

        Location origin = getLocationById(request.getOriginId());
        Location destination = getLocationById(request.getDestinationId());

        route.setOrigin(origin);
        route.setDestination(destination);

        Route updatedRoute = routeRepository.save(route);

        routeStopRepository.deleteAll(
                routeStopRepository.findByRoute_RouteIdOrderByStopOrderAsc(routeId)
        );
        routeStopRepository.flush();

        createRouteStops(
                updatedRoute,
                origin,
                destination,
                request.getIntermediateStopIds()
        );

        return enrichRouteResponse(updatedRoute);
    }

    // ================= DELETE =================
    @Override
    public void deleteRoute(Integer routeId) {

        Route route = routeRepository.findById(routeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Route not found with id: " + routeId)
                );

        routeRepository.delete(route);
    }

    // ================= GET ALL =================
    @Override
    public Page<RouteResponseDTO> getAllRoutesForUI(Pageable pageable) {
        return routeRepository.findAll(pageable)
                .map(this::enrichRouteResponse);
    }

    // ================= HELPER =================

    private void validateRouteRequest(RouteRequestDTO request) {

        if (request.getOriginId() == null || request.getDestinationId() == null) {
            throw new BadRequestException("Origin and destination must not be null");
        }

        if (request.getOriginId().equals(request.getDestinationId())) {
            throw new BadRequestException("Origin and destination must not be the same");
        }
    }

    private Location getLocationById(Integer id) {
        return locationRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Location not found with id: " + id)
                );
    }

    private void createRouteStops(
            Route route,
            Location origin,
            Location destination,
            List<Integer> intermediateStopIds
    ) {

        int order = 1;

        saveStop(route, origin, order++, StopType.ORIGIN);

        if (intermediateStopIds != null && !intermediateStopIds.isEmpty()) {
            for (Integer stopId : intermediateStopIds) {
                saveStop(
                        route,
                        getLocationById(stopId),
                        order++,
                        StopType.INTERMEDIATE
                );
            }
        }

        saveStop(route, destination, order, StopType.DESTINATION);
    }

    private void saveStop(
            Route route,
            Location location,
            int order,
            StopType type
    ) {

        RouteStop stop = new RouteStop();
        stop.setRoute(route);
        stop.setLocation(location);
        stop.setStopOrder(order);
        stop.setStopType(type.dbValue());

        routeStopRepository.save(stop);
    }

    private RouteResponseDTO enrichRouteResponse(Route route) {

        RouteResponseDTO dto = routeMapper.toResponseDTO(route);

        List<RouteStop> stops =
                routeStopRepository.findByRoute_RouteIdOrderByStopOrderAsc(route.getRouteId());

        dto.setStopNames(
                stops.stream()
                        .map(s -> s.getLocation().getLocationName())
                        .collect(Collectors.toList())
        );

        dto.setTotalStops(stops.size());

        return dto;
    }
}
