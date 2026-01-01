package com.example.Fuba_BE.service.Trip;

import com.example.Fuba_BE.domain.entity.Trip;
import com.example.Fuba_BE.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TripService implements ITripService {
    @Autowired
    private TripRepository tripRepository;

    public List<Trip> getAllTrips() {
        return tripRepository.findAll();
    }

    public Trip createTrip(Trip newTrip) {
        return tripRepository.save(newTrip);
    }
}
