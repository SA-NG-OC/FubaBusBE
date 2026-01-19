package com.example.Fuba_BE.service.Trip;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.Fuba_BE.domain.entity.Trip;
import com.example.Fuba_BE.dto.Trip.CompleteTripRequestDTO;
import com.example.Fuba_BE.dto.Trip.PassengerOnTripResponseDTO;
import com.example.Fuba_BE.dto.Trip.TicketDetailResponseDTO;
import com.example.Fuba_BE.dto.Trip.TripCreateRequestDTO;
import com.example.Fuba_BE.dto.Trip.TripDetailedResponseDTO;
import com.example.Fuba_BE.dto.Trip.TripUpdateRequestDTO;

public interface ITripService {
        List<LocalDate> getDaysWithTrips(LocalDate startDate, LocalDate endDate);

        List<Trip> getTripsDetailsByDate(LocalDate date);

        void updateTripStatus(Integer tripId, String status, String note);

        Page<Trip> getTripsByFilters(String status, LocalDate date, Integer originId, Integer destId,
                        Pageable pageable);

        Trip createTrip(TripCreateRequestDTO request);

        void deleteTrip(Integer tripId);

        Page<Trip> getTripsForDriver(Integer driverId, String status, LocalDate startDate, LocalDate endDate,
                        Pageable pageable);

        List<PassengerOnTripResponseDTO> getPassengersOnTrip(Integer tripId);

        TripDetailedResponseDTO enrichTripStats(TripDetailedResponseDTO dto, Integer tripId);

        Trip updateTrip(Integer tripId, TripUpdateRequestDTO request);

        Page<TripDetailedResponseDTO> getAllTrips(int page, int size, String sortBy, String sortDir,
                        String search, Integer originId, Integer destId, Integer routeId,
                        Double minPrice, Double maxPrice, LocalDate date,
                        List<String> timeRanges, List<String> vehicleTypes,
                        Integer minAvailableSeats, List<String> statuses);

        // Driver-specific endpoints
        TicketDetailResponseDTO getTicketDetail(Integer ticketId);

        void completeTrip(Integer tripId, CompleteTripRequestDTO request);

        Page<Trip> getMyTripsForDriver(Integer userId, String status, LocalDate startDate, LocalDate endDate,
                        Pageable pageable);

        TripDetailedResponseDTO getTripDetailById(Integer tripId);
}