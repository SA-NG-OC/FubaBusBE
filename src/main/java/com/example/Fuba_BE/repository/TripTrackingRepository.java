package com.example.Fuba_BE.repository;

import com.example.Fuba_BE.domain.entity.TripTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripTrackingRepository extends JpaRepository<TripTracking, Integer> {

    // Lấy bản ghi tracking mới nhất của 1 trip
    Optional<TripTracking> findTopByTrip_TripIdOrderByRecordedAtDesc(Integer tripId);

    // Lấy toàn bộ lịch sử tracking của 1 trip
    List<TripTracking> findByTrip_TripIdOrderByRecordedAtDesc(Integer tripId);

    // Lấy tracking theo traffic status (VD: Heavy, Normal)
    List<TripTracking> findByTrafficStatus(String trafficStatus);

    // Custom query: lấy tracking mới nhất cho nhiều trip
    @Query("""
        SELECT tt FROM TripTracking tt
        WHERE tt.trip.tripId IN :tripIds
        AND tt.recordedAt = (
            SELECT MAX(t2.recordedAt)
            FROM TripTracking t2
            WHERE t2.trip.tripId = tt.trip.tripId
        )
    """)
    List<TripTracking> findLatestTrackingByTripIds(@Param("tripIds") List<Integer> tripIds);
}
