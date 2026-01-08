package com.example.Fuba_BE.service.Vehicle;

import com.example.Fuba_BE.dto.Vehicle.VehicleRequestDTO;
import com.example.Fuba_BE.dto.Vehicle.VehicleResponseDTO;
import com.example.Fuba_BE.dto.Vehicle.VehicleSelectionDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IVehicleService {
    List<VehicleSelectionDTO> getAllVehiclesForSelection();

    Page<VehicleResponseDTO> getAllVehicles(String keyword, Pageable pageable);

    VehicleResponseDTO getVehicleById(Integer id);

    VehicleResponseDTO createVehicle(VehicleRequestDTO vehicleRequestDTO);

    VehicleResponseDTO updateVehicle(Integer id, VehicleRequestDTO vehicleRequestDTO);

    void deleteVehicle(Integer id);
}
