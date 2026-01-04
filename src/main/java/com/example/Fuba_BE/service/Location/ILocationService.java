package com.example.Fuba_BE.service.Location;

import com.example.Fuba_BE.dto.Location.LocationResponseDTO;
import java.util.List;

public interface ILocationService {
    List<LocationResponseDTO> getAllLocations();
}
