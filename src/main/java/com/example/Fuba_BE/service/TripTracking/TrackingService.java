package com.example.Fuba_BE.service.TripTracking;

import com.example.Fuba_BE.domain.entity.*;
import com.example.Fuba_BE.dto.TripTracking.LocationUpdateReq;
import com.example.Fuba_BE.dto.TripTracking.TripRouteResponse;
import com.example.Fuba_BE.repository.TripRepository;
import com.example.Fuba_BE.repository.TripTrackingRepository;
import com.example.Fuba_BE.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrackingService implements ITrackingService {

    private final TripTrackingRepository trackingRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;

    @Async
    @Transactional
    public void saveLocationHistory(LocationUpdateReq req) {
        try {
            // Logic: Chỉ lưu nếu tripId và driverId hợp lệ
            if (req.getTripId() == null || req.getDriverId() == null) return;

            Trip trip = tripRepository.findById(req.getTripId()).orElse(null);
            User user = userRepository.findById(req.getDriverId()).orElse(null);

            if (trip != null && user != null) {
                TripTracking tracking = new TripTracking();
                tracking.setTrip(trip);
                tracking.setRecordedBy(user);
                tracking.setCurrentLatitude(req.getLatitude());
                tracking.setCurrentLongitude(req.getLongitude());
                tracking.setSpeed(req.getSpeed());
                tracking.setDirection(req.getDirection());

                tracking.setRecordedAt(java.time.LocalDateTime.now());

                trackingRepository.save(tracking);
            }
        } catch (Exception e) {
            System.err.println("Lỗi lưu GPS: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true) // Quan trọng để fetch dữ liệu Lazy (Route, Location)
    public TripRouteResponse getTripRouteInfo(Integer tripId) {
        // 1. Tìm chuyến đi
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found with id: " + tripId));

        // 2. Lấy Route từ Trip
        Route route = trip.getRoute();
        if (route == null) {
            throw new RuntimeException("Route information is missing for this trip");
        }

        // 3. Map dữ liệu sang DTO (Mapper thủ công)
        return TripRouteResponse.builder()
                .tripId(trip.getTripId())
                .routeName(route.getRouteName())
                .origin(mapToPointInfo(route.getOrigin()))          // Map điểm đi
                .destination(mapToPointInfo(route.getDestination())) // Map điểm đến
                .build();
    }

    // Hàm phụ để convert Location Entity sang DTO nhỏ
    private TripRouteResponse.PointInfo mapToPointInfo(Location location) {
        if (location == null) return null;
        return TripRouteResponse.PointInfo.builder()
                .locationName(location.getLocationName())
                .address(location.getAddress())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .build();
    }
}

