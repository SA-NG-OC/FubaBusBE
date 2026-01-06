package com.example.Fuba_BE.service.Trip;

import com.example.Fuba_BE.domain.entity.Trip;
import com.example.Fuba_BE.dto.Trip.TripCreateRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface ITripService {
    List<LocalDate> getDaysWithTrips(LocalDate startDate, LocalDate endDate);

    // Trả về List<Trip> thay vì DTO
    List<Trip> getTripsDetailsByDate(LocalDate date);

    void updateTripStatus(Integer tripId, String status);

    // Trả về Page<Trip> thay vì Page<DTO>
    Page<Trip> getTripsByFilters(String status, LocalDate date, Integer originId, Integer destId, Pageable pageable);

    // Trả về Trip thay vì DTO
    Trip createTrip(TripCreateRequestDTO request);

    void deleteTrip(Integer tripId);
}
