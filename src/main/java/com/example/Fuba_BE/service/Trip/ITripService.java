package com.example.Fuba_BE.service.Trip;

import com.example.Fuba_BE.domain.entity.Trip;
import com.example.Fuba_BE.dto.Trip.PassengerOnTripResponseDTO;
import com.example.Fuba_BE.dto.Trip.TripCreateRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface ITripService {
    List<LocalDate> getDaysWithTrips(LocalDate startDate, LocalDate endDate);

    // Trả về List<Trip> thay vì DTO
    List<Trip> getTripsDetailsByDate(LocalDate date);

    void updateTripStatus(Integer tripId, String status, String note);

    // Trả về Page<Trip> thay vì Page<DTO>
    Page<Trip> getTripsByFilters(String status, LocalDate date, Integer originId, Integer destId, Pageable pageable);

    // Trả về Trip thay vì DTO
    Trip createTrip(TripCreateRequestDTO request);

    void deleteTrip(Integer tripId);

    Page<Trip> getTripsForDriver(Integer driverId, String status, Pageable pageable);

    /**
     * Get list of all passengers on a trip with seat info and ticket status.
     * @param tripId The trip ID
     * @return List of passengers with seat and ticket info
     */
    List<PassengerOnTripResponseDTO> getPassengersOnTrip(Integer tripId);
}
