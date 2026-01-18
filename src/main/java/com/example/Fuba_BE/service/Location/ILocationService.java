package com.example.Fuba_BE.service.Location;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.Fuba_BE.dto.Location.CreateLocationRequestDTO;
import com.example.Fuba_BE.dto.Location.LocationResponseDTO;
import com.example.Fuba_BE.dto.Location.UpdateLocationRequestDTO;

public interface ILocationService {
    List<LocationResponseDTO> getAllLocations();

    Page<LocationResponseDTO> getLocations(Pageable pageable, String search, String province);

    LocationResponseDTO getLocationById(Integer id);

    LocationResponseDTO createLocation(CreateLocationRequestDTO request);

    LocationResponseDTO updateLocation(UpdateLocationRequestDTO request);

    void deleteLocation(Integer id);

    List<String> getDistinctProvinces();
}
