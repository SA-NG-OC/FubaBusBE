package com.example.Fuba_BE.service.Vehicle;

import com.example.Fuba_BE.domain.entity.Vehicle;
import com.example.Fuba_BE.dto.Vehicle.VehicleSelectionDTO;
import com.example.Fuba_BE.mapper.SelectionMapper;
import com.example.Fuba_BE.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleService implements IVehicleService {

    private final VehicleRepository vehicleRepository;
    private final SelectionMapper selectionMapper;

    @Override
    public List<VehicleSelectionDTO> getAllVehiclesForSelection() {
        // Chỉ lấy xe có trạng thái "Hoàn thiện" hoặc "Hoạt động" tùy data của bạn
        List<Vehicle> vehicles = vehicleRepository.findByStatus("Hoàn thiện");

        return vehicles.stream()
                .map(selectionMapper::toVehicleSelectionDTO)
                .toList();
    }
}
