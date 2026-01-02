package com.example.Fuba_BE.service.Trip;

import com.example.Fuba_BE.domain.entity.Trip;
import com.example.Fuba_BE.dto.Trip.TripCalendarDTO;
import com.example.Fuba_BE.exception.BadRequestException;
import com.example.Fuba_BE.mapper.TripMapper;
import com.example.Fuba_BE.dto.Trip.TripDetailedResponseDTO;
import com.example.Fuba_BE.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TripService implements ITripService {
    @Autowired
    private TripRepository tripRepository;
    private final TripMapper tripMapper;

    public List<Trip> getAllTrips() {
        return tripRepository.findAll();
    }

    public Trip createTrip(Trip newTrip) {
        return tripRepository.save(newTrip);
    }

    @Transactional(readOnly = true)
    public List<LocalDate> getDaysWithTrips(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date cannot be after end date");
        }
        // Chuyển đổi LocalDate sang LocalDateTime để query
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        return tripRepository.findDistinctTripDates(start, end);
    }

    @Transactional(readOnly = true)
    public List<TripDetailedResponseDTO> getTripsDetailsByDate(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        List<Trip> trips = tripRepository.findAllTripsByDate(startOfDay, endOfDay);

        // Dùng Mapper để chuyển đổi Entity -> DTO
        return trips.stream()
                .map(tripMapper::toDetailedDTO)
                .collect(Collectors.toList());
    }
}
