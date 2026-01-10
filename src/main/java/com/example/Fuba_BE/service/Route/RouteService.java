package com.example.Fuba_BE.service.Route;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RouteService implements IRouteService {

    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final LocationRepository locationRepository;
    private final RouteMapper routeMapper;
    private final SelectionMapper selectionMapper;

    // Sử dụng SLF4J chuẩn của Spring Boot
    private static final Logger log = LoggerFactory.getLogger(RouteService.class);

    // --- Helper: Tìm Location theo tên ---
    private Location getLocationByName(String locationName) {
        return locationRepository.findByLocationName(locationName)
                .orElseThrow(() -> {
                    log.error("❌ Lỗi: Không tìm thấy địa điểm có tên '{}'", locationName);
                    return new ResourceNotFoundException("Không tìm thấy địa điểm: " + locationName);
                });
    }

    @Override
    public RouteResponseDTO createRoute(RouteRequestDTO request) {
        log.info("🚀 Bắt đầu tạo Route mới: {} -> {}", request.getOriginName(), request.getDestinationName());
        try {
            validateRouteRequest(request);
            Route route = routeMapper.toEntity(request);

            Location origin = getLocationByName(request.getOriginName());
            Location destination = getLocationByName(request.getDestinationName());

            route.setOrigin(origin);
            route.setDestination(destination);
            if (route.getStatus() == null) route.setStatus("Active");

            Route savedRoute = routeRepository.save(route);
            log.info("✅ Đã lưu Route entity với ID: {}", savedRoute.getRouteId());

            createRouteStops(savedRoute, origin, destination, request.getIntermediateStopNames());
            log.info("✅ Đã tạo xong các RouteStop cho Route ID: {}", savedRoute.getRouteId());

            return enrichSingleRoute(savedRoute);

        } catch (Exception e) {
            log.error("🔥 Lỗi nghiêm trọng khi tạo Route: {}", e.getMessage(), e);
            throw e; // Ném tiếp lỗi để Controller xử lý
        }
    }

    @Override
    public RouteResponseDTO updateRoute(Integer routeId, RouteRequestDTO request) {
        log.info("🔄 Bắt đầu cập nhật Route ID: {}", routeId);
        try {
            validateRouteRequest(request);
            Route route = routeRepository.findById(routeId)
                    .orElseThrow(() -> {
                        log.error("❌ Không tìm thấy Route ID: {} để update", routeId);
                        return new ResourceNotFoundException("Route not found: " + routeId);
                    });

            routeMapper.updateEntityFromDto(request, route);
            Location origin = getLocationByName(request.getOriginName());
            Location destination = getLocationByName(request.getDestinationName());
            route.setOrigin(origin);
            route.setDestination(destination);

            Route updatedRoute = routeRepository.save(route);
            log.info("✅ Đã cập nhật thông tin cơ bản Route ID: {}", routeId);

            // Logic xóa đi tạo lại
            log.info("🧹 Đang xóa các RouteStop cũ của Route ID: {}", routeId);
            routeStopRepository.deleteAll(routeStopRepository.findByRoute_RouteIdOrderByStopOrderAsc(routeId));
            routeStopRepository.flush();

            log.info("🛠 Đang tạo lại các RouteStop mới...");
            createRouteStops(updatedRoute, origin, destination, request.getIntermediateStopNames());

            return enrichSingleRoute(updatedRoute);

        } catch (Exception e) {
            log.error("🔥 Lỗi nghiêm trọng khi update Route ID {}: {}", routeId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void deleteRoute(Integer routeId) {
        log.info("🗑 Yêu cầu xóa Route ID: {}", routeId);
        try {
            if (!routeRepository.existsById(routeId)) {
                log.error("❌ Không thể xóa vì không tìm thấy Route ID: {}", routeId);
                throw new ResourceNotFoundException("Route not found: " + routeId);
            }
            routeRepository.deleteById(routeId);
            log.info("✅ Đã xóa thành công Route ID: {}", routeId);
        } catch (Exception e) {
            log.error("🔥 Lỗi khi xóa Route ID {}: {}", routeId, e.getMessage(), e);
            throw e;
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
        log.info("🔎 Tìm kiếm Routes với từ khóa: '{}'", keyword);
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
            log.warn("⚠️ Validate thất bại: Điểm đi hoặc đến bị null");
            throw new BadRequestException("Điểm đi và điểm đến không được để trống");
        }
        if (request.getOriginName().equalsIgnoreCase(request.getDestinationName())) {
            log.warn("⚠️ Validate thất bại: Điểm đi trùng điểm đến ({})", request.getOriginName());
            throw new BadRequestException("Điểm đi và điểm đến không được trùng nhau");
        }
    }

    private void createRouteStops(Route route, Location origin, Location destination, List<String> intermediateStopNames) {
        log.debug("...Đang tạo stops cho Route ID: {}", route.getRouteId());
        int orderCounter = 1;

        // 1. Start Stop
        RouteStop startStop = createStop(route, origin, orderCounter++, "ORIGIN", true, false, BigDecimal.ZERO, 0);
        routeStopRepository.save(startStop);
        log.debug("   + Đã tạo Origin Stop: {}", origin.getLocationName());

        // 2. Intermediate Stops
        if (intermediateStopNames != null) {
            for (String stopName : intermediateStopNames) {
                String normalized = stopName.trim();
                if (normalized.equalsIgnoreCase(origin.getLocationName()) ||
                        normalized.equalsIgnoreCase(destination.getLocationName())) {
                    log.warn("   ⚠️ Bỏ qua stop trùng lặp: {}", stopName);
                    continue;
                }

                Location loc = getLocationByName(stopName);
                RouteStop stop = createStop(route, loc, orderCounter++, "INTERMEDIATE", true, true, BigDecimal.ZERO, 0);
                routeStopRepository.save(stop);
                log.debug("   + Đã tạo Intermediate Stop: {}", stopName);
            }
        }

        // 3. End Stop
        RouteStop endStop = createStop(route, destination, orderCounter, "DESTINATION", false, true, route.getDistance(), route.getEstimatedDuration());
        routeStopRepository.save(endStop);
        log.debug("   + Đã tạo Destination Stop: {}", destination.getLocationName());
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