package com.example.Fuba_BE.service.TripTracking;

import com.example.Fuba_BE.domain.entity.Trip;
import com.example.Fuba_BE.domain.entity.TripTracking;
import com.example.Fuba_BE.domain.entity.User;
import com.example.Fuba_BE.dto.TripTracking.LocationUpdateReq;
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
}

