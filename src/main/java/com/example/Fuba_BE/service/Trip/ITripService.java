package com.example.Fuba_BE.service.Trip;

import com.example.Fuba_BE.dto.Trip.TripCalendarDTO;
import com.example.Fuba_BE.dto.Trip.TripDetailedResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ITripService {
    List<LocalDate> getDaysWithTrips(LocalDate startDate, LocalDate endDate);
    List<TripDetailedResponseDTO> getTripsDetailsByDate(LocalDate date);
    Page<TripDetailedResponseDTO> getTripsByStatus(String status, Pageable pageable);
    void updateTripStatus(Integer tripId, String status);

}
