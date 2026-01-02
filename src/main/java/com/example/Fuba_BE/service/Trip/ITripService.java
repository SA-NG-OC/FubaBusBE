package com.example.Fuba_BE.service.Trip;

import com.example.Fuba_BE.dto.Trip.TripCalendarDTO;
import com.example.Fuba_BE.dto.Trip.TripDetailedResponseDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ITripService {
    List<LocalDate> getDaysWithTrips(LocalDate startDate, LocalDate endDate);
    List<TripDetailedResponseDTO> getTripsDetailsByDate(LocalDate date);
}
