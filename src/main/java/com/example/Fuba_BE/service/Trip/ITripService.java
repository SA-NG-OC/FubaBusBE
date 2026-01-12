package com.example.Fuba_BE.service.Trip;

import com.example.Fuba_BE.domain.entity.Trip;
import com.example.Fuba_BE.dto.Trip.PassengerOnTripResponseDTO;
import com.example.Fuba_BE.dto.Trip.TripCreateRequestDTO;
import com.example.Fuba_BE.dto.Trip.TripDetailedResponseDTO;
import com.example.Fuba_BE.dto.Trip.TripUpdateRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface ITripService {
    List<LocalDate> getDaysWithTrips(LocalDate startDate, LocalDate endDate);

    List<Trip> getTripsDetailsByDate(LocalDate date);

    void updateTripStatus(Integer tripId, String status, String note);

    Page<Trip> getTripsByFilters(String status, LocalDate date, Integer originId, Integer destId, Pageable pageable);

    Trip createTrip(TripCreateRequestDTO request);

    void deleteTrip(Integer tripId);

    Page<Trip> getTripsForDriver(Integer driverId, String status, Pageable pageable);

    List<PassengerOnTripResponseDTO> getPassengersOnTrip(Integer tripId);

    TripDetailedResponseDTO enrichTripStats(TripDetailedResponseDTO dto, Integer tripId);

    Trip updateTrip(Integer tripId, TripUpdateRequestDTO request);
}