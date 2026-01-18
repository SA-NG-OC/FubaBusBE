package com.example.Fuba_BE.service.Vehicle;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.Fuba_BE.dto.Vehicle.VehicleTypeRequestDTO;
import com.example.Fuba_BE.dto.Vehicle.VehicleTypeResponseDTO;

public interface IVehicleTypeService {

    /**
     * Get all vehicle types with pagination
     */
    Page<VehicleTypeResponseDTO> getAllVehicleTypes(Pageable pageable);

    /**
     * Get all vehicle types without pagination (for selection dropdowns)
     */
    List<VehicleTypeResponseDTO> getAllVehicleTypesForSelection();

    /**
     * Get vehicle type by ID
     */
    VehicleTypeResponseDTO getVehicleTypeById(Integer id);

    /**
     * Create new vehicle type
     */
    VehicleTypeResponseDTO createVehicleType(VehicleTypeRequestDTO request);

    /**
     * Update existing vehicle type
     */
    VehicleTypeResponseDTO updateVehicleType(Integer id, VehicleTypeRequestDTO request);

    /**
     * Delete vehicle type
     */
    void deleteVehicleType(Integer id);
}
