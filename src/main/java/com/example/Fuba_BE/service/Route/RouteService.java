package com.example.Fuba_BE.service.Route;

import com.example.Fuba_BE.domain.entity.Location;
import com.example.Fuba_BE.domain.entity.Route;
import com.example.Fuba_BE.domain.entity.RouteStop;
import com.example.Fuba_BE.dto.Routes.RouteRequestDTO;
import com.example.Fuba_BE.dto.Routes.RouteResponseDTO;
import com.example.Fuba_BE.dto.Routes.RouteStopResponseDTO;
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

import java.math.BigDecimal;
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

    // --- Helper: Tìm Location theo tên chính xác ---
    private Location getLocationByName(String locationName) {
        return locationRepository.findByLocationName(locationName)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa điểm với tên: " + locationName));
    }

    // ================= CREATE =================
    @Override
    public RouteResponseDTO createRoute(RouteRequestDTO request) {

        // 1. Validate logic nghiệp vụ
        validateRouteRequest(request);

        // 2. Map dữ liệu cơ bản từ DTO sang Entity
        Route route = routeMapper.toEntity(request);

        // 3. Tìm Location Entity dựa trên tên
        Location origin = getLocationByName(request.getOriginName());
        Location destination = getLocationByName(request.getDestinationName());

        // 4. Gán Location vào Route
        route.setOrigin(origin);
        route.setDestination(destination);

        // Gán trạng thái mặc định
        if (route.getStatus() == null) {
            route.setStatus("Hoạt động");
        }

        // 5. Lưu Route
        Route savedRoute = routeRepository.save(route);

        // 6. Tạo các trạm dừng (RouteStops)
        createRouteStops(
                savedRoute,
                origin,
                destination,
                request.getIntermediateStopNames()
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

        // Update các field cơ bản
        routeMapper.updateEntityFromDto(request, route);

        // Tìm Location mới theo tên
        Location origin = getLocationByName(request.getOriginName());
        Location destination = getLocationByName(request.getDestinationName());

        route.setOrigin(origin);
        route.setDestination(destination);

        Route updatedRoute = routeRepository.save(route);

        // Xóa các trạm dừng cũ
        routeStopRepository.deleteAll(
                routeStopRepository.findByRoute_RouteIdOrderByStopOrderAsc(routeId)
        );
        routeStopRepository.flush(); // Đẩy lệnh xóa xuống DB ngay lập tức để tránh lỗi Unique index nếu có

        // Tạo lại các trạm dừng mới
        createRouteStops(
                updatedRoute,
                origin,
                destination,
                request.getIntermediateStopNames()
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

    // ================= GET ALL & SEARCH =================
    @Override
    public Page<RouteResponseDTO> getAllRoutesForUI(Pageable pageable) {
        return routeRepository.findAll(pageable)
                .map(this::enrichRouteResponse);
    }

    @Override
    public Page<RouteResponseDTO> searchRoutes(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllRoutesForUI(pageable);
        }
        return routeRepository
                .searchRoutes(keyword.trim(), pageable)
                .map(this::enrichRouteResponse);
    }

    @Override
    public List<RouteStopResponseDTO> getAllRouteStop() {
        return routeStopRepository.findAllRouteStopBasic();
    }

    // ================= PRIVATE HELPER METHODS =================

    // Validate dựa trên Tên thay vì ID
    private void validateRouteRequest(RouteRequestDTO request) {
        if (request.getOriginName() == null || request.getDestinationName() == null) {
            throw new BadRequestException("Điểm đi và điểm đến không được để trống");
        }

        if (request.getOriginName().equalsIgnoreCase(request.getDestinationName())) {
            throw new BadRequestException("Điểm đi và điểm đến không được trùng nhau");
        }
    }

    // Hàm tạo danh sách trạm dừng chi tiết
    private void createRouteStops(Route route, Location origin, Location destination, List<String> intermediateStopNames) {

        int orderCounter = 1;

        // 1. Tạo điểm đầu (Start Point)
        RouteStop startStop = new RouteStop();
        startStop.setRoute(route);
        startStop.setLocation(origin);
        startStop.setStopOrder(orderCounter++);
        startStop.setStopType("ORIGIN");
        startStop.setStopName(origin.getLocationName());
        startStop.setStopAddress(origin.getAddress());
        startStop.setLatitude(origin.getLatitude());
        startStop.setLongitude(origin.getLongitude());
        startStop.setIsPickupPoint(true);  // Điểm đầu chỉ đón
        startStop.setIsDropoffPoint(false);
        startStop.setDistanceFromOrigin(BigDecimal.ZERO);
        startStop.setEstimatedTime(0);

        routeStopRepository.save(startStop);

        // 2. Tạo các điểm giữa (Intermediate Stops)
        if (intermediateStopNames != null && !intermediateStopNames.isEmpty()) {

            for (String stopName : intermediateStopNames) {

                // Chuẩn hóa
                String normalized = stopName.trim().toLowerCase();

                // BỎ QUA nếu trùng origin hoặc destination
                if (normalized.equals(origin.getLocationName().trim().toLowerCase()) ||
                        normalized.equals(destination.getLocationName().trim().toLowerCase())) {
                    continue;
                }

                Location loc = getLocationByName(stopName);

                RouteStop stop = new RouteStop();
                stop.setRoute(route);
                stop.setLocation(loc);
                stop.setStopOrder(orderCounter++);
                stop.setStopType("INTERMEDIATE");
                stop.setStopName(loc.getLocationName());
                stop.setStopAddress(loc.getAddress());
                stop.setLatitude(loc.getLatitude());
                stop.setLongitude(loc.getLongitude());
                stop.setIsPickupPoint(true);
                stop.setIsDropoffPoint(true);
                stop.setDistanceFromOrigin(BigDecimal.ZERO);
                stop.setEstimatedTime(0);

                routeStopRepository.save(stop);
            }
        }

        // 3. Tạo điểm cuối (End Point)
        RouteStop endStop = new RouteStop();
        endStop.setRoute(route);
        endStop.setLocation(destination);
        endStop.setStopOrder(orderCounter);
        endStop.setStopType("DESTINATION");
        endStop.setStopName(destination.getLocationName());
        endStop.setStopAddress(destination.getAddress());
        endStop.setLatitude(destination.getLatitude());
        endStop.setLongitude(destination.getLongitude());
        endStop.setIsPickupPoint(false);
        endStop.setIsDropoffPoint(true); // Điểm cuối chỉ trả
        endStop.setDistanceFromOrigin(route.getDistance());
        endStop.setEstimatedTime(route.getEstimatedDuration());

        routeStopRepository.save(endStop);
    }

    // Map Entity sang Response DTO kèm danh sách tên trạm dừng
    private RouteResponseDTO enrichRouteResponse(Route route) {
        RouteResponseDTO dto = routeMapper.toResponseDTO(route);

        List<RouteStop> stops = routeStopRepository.findByRoute_RouteIdOrderByStopOrderAsc(route.getRouteId());

        dto.setStopNames(
                stops.stream()
                        .map(s -> s.getLocation().getLocationName())
                        .collect(Collectors.toList())
        );

        dto.setTotalStops(stops.size());
        return dto;
    }
}