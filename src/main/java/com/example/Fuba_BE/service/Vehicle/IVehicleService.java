package com.example.Fuba_BE.service.Vehicle;

import com.example.Fuba_BE.dto.Vehicle.VehicleSelectionDTO;
import java.util.List;

public interface IVehicleService {
    List<VehicleSelectionDTO> getAllVehiclesForSelection();
}
