package com.example.Fuba_BE.service.Location;

import com.example.Fuba_BE.dto.Location.LocationResponseDTO;
import com.example.Fuba_BE.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService implements ILocationService {

    private final LocationRepository locationRepository;

    @Override
    public List<LocationResponseDTO> getAllLocations() {
        return locationRepository.findAllBasic();
    }
}
