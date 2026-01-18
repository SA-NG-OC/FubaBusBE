package com.example.Fuba_BE.service.Location;

import com.example.Fuba_BE.domain.entity.Location;
import com.example.Fuba_BE.dto.Location.CreateLocationRequestDTO;
import com.example.Fuba_BE.dto.Location.LocationResponseDTO;
import com.example.Fuba_BE.dto.Location.UpdateLocationRequestDTO;
import com.example.Fuba_BE.exception.NotFoundException;
import com.example.Fuba_BE.mapper.LocationMapper;
import com.example.Fuba_BE.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class LocationService implements ILocationService {

    private final LocationRepository locationRepository;
    private final LocationMapper locationMapper;

    @Override
    public List<LocationResponseDTO> getAllLocations() {
        return locationRepository.findAllBasic();
    }

    @Override
    public Page<LocationResponseDTO> getLocations(Pageable pageable, String search, String province) {
        log.info("Getting locations with search: {}, province: {}", search, province);

        Page<Location> locations;

        if (search != null && !search.trim().isEmpty() && province != null && !province.trim().isEmpty()) {
            locations = locationRepository.findByLocationNameContainingIgnoreCaseAndProvinceContainingIgnoreCase(
                    search.trim(), province.trim(), pageable);
        } else if (search != null && !search.trim().isEmpty()) {
            locations = locationRepository.findByLocationNameContainingIgnoreCaseOrAddressContainingIgnoreCase(
                    search.trim(), search.trim(), pageable);
        } else if (province != null && !province.trim().isEmpty()) {
            locations = locationRepository.findByProvinceContainingIgnoreCase(province.trim(), pageable);
        } else {
            locations = locationRepository.findAll(pageable);
        }

        return locations.map(locationMapper::toResponseDTO);
    }

    @Override
    public LocationResponseDTO getLocationById(Integer id) {
        log.info("Getting location by ID: {}", id);
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy địa điểm với ID: " + id));

        return locationMapper.toResponseDTO(location);
    }

    @Override
    @Transactional
    public LocationResponseDTO createLocation(CreateLocationRequestDTO request) {
        log.info("Creating new location: {}", request.getLocationName());

        Location location = locationMapper.toEntity(request);
        location = locationRepository.save(location);

        log.info("Created location with ID: {}", location.getLocationId());
        return locationMapper.toResponseDTO(location);
    }

    @Override
    @Transactional
    public LocationResponseDTO updateLocation(UpdateLocationRequestDTO request) {
        log.info("Updating location with ID: {}", request.getLocationId());

        Location existingLocation = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy địa điểm với ID: " + request.getLocationId()));

        // Update only non-null fields
        if (request.getLocationName() != null) {
            existingLocation.setLocationName(request.getLocationName());
        }
        if (request.getAddress() != null) {
            existingLocation.setAddress(request.getAddress());
        }
        if (request.getProvince() != null) {
            existingLocation.setProvince(request.getProvince());
        }
        if (request.getLatitude() != null) {
            existingLocation.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            existingLocation.setLongitude(request.getLongitude());
        }

        existingLocation = locationRepository.save(existingLocation);

        log.info("Updated location with ID: {}", existingLocation.getLocationId());
        return locationMapper.toResponseDTO(existingLocation);
    }

    @Override
    @Transactional
    public void deleteLocation(Integer id) {
        log.info("Deleting location with ID: {}", id);

        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy địa điểm với ID: " + id));

        locationRepository.delete(location);
        log.info("Deleted location with ID: {}", id);
    }

    @Override
    public List<String> getDistinctProvinces() {
        log.info("Getting distinct provinces");
        return locationRepository.findDistinctProvinces();
    }
}
