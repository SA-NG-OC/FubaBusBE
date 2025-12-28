package com.example.Fuba_BE.repository;

import com.example.Fuba_BE.domain.TripSeat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripSeatRepository extends JpaRepository<TripSeat, Integer> {
    List<TripSeat> findByTrip_TripIdOrderByFloorNumberAscSeatNumberAsc(Integer tripId);

    boolean existsByTrip_TripId(Integer tripId);

    void deleteByTrip_TripId(Integer tripId);
}
