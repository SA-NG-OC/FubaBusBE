package com.example.Fuba_BE.service.Vehicle;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Fuba_BE.domain.entity.Vehicle;
import com.example.Fuba_BE.domain.entity.VehicleType;
import com.example.Fuba_BE.dto.Vehicle.VehicleRequestDTO;
import com.example.Fuba_BE.dto.Vehicle.VehicleResponseDTO;
import com.example.Fuba_BE.dto.Vehicle.VehicleSelectionDTO;
import com.example.Fuba_BE.dto.Vehicle.VehicleStatsDTO;
import com.example.Fuba_BE.exception.NotFoundException;
import com.example.Fuba_BE.mapper.SelectionMapper;
import com.example.Fuba_BE.mapper.VehicleMapper;
import com.example.Fuba_BE.repository.VehicleRepository;
import com.example.Fuba_BE.repository.VehicleTypeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VehicleService implements IVehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final SelectionMapper selectionMapper;
    private final VehicleMapper vehicleMapper;

    @Override
    public List<VehicleSelectionDTO> getAllVehiclesForSelection() {
        List<Vehicle> vehicles = vehicleRepository.findAllWithVehicleType();

        return vehicles.stream()
                .map(selectionMapper::toVehicleSelectionDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehicleResponseDTO> getAllVehicles(String keyword, String status, Integer routeId, Pageable pageable) {
        Page<Vehicle> page;

        if (routeId != null) {
            // Filter by route
            page = vehicleRepository.findByRouteId(routeId, keyword, status, pageable);
        } else if (status != null && !status.isBlank()) {
            // Filter by status only
            if (keyword == null || keyword.isBlank()) {
                page = vehicleRepository.findByStatusIgnoreCase(status, pageable);
            } else {
                page = vehicleRepository.findByKeywordAndStatus(keyword, status, pageable);
            }
        } else if (keyword != null && !keyword.isBlank()) {
            // Filter by keyword only
            page = vehicleRepository.findByLicensePlateContainingIgnoreCase(keyword, pageable);
        } else {
            // No filters
            page = vehicleRepository.findAll(pageable);
        }

        return page.map(vehicleMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponseDTO getVehicleById(Integer id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Vehicle not found with id: " + id));
        return vehicleMapper.toResponseDTO(vehicle);
    }

    @Override
    @Transactional
    public VehicleResponseDTO createVehicle(VehicleRequestDTO request) {
        VehicleType vehicleType = vehicleTypeRepository.findById(request.getTypeId())
                .orElseThrow(() -> new NotFoundException("VehicleType not found with id: " + request.getTypeId()));

        Vehicle vehicle = new Vehicle();
        vehicle.setLicensePlate(request.getLicensePlate());
        vehicle.setVehicleType(vehicleType);
        vehicle.setInsuranceNumber(request.getInsuranceNumber());
        vehicle.setInsuranceExpiry(request.getInsuranceExpiry());
        if (request.getStatus() != null) {
            vehicle.setStatus(request.getStatus());
        }

        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        return vehicleMapper.toResponseDTO(savedVehicle);
    }

    @Override
    @Transactional
    public VehicleResponseDTO updateVehicle(Integer id, VehicleRequestDTO request) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Vehicle not found with id: " + id));

        VehicleType vehicleType = vehicleTypeRepository.findById(request.getTypeId())
                .orElseThrow(() -> new NotFoundException("VehicleType not found with id: " + request.getTypeId()));

        vehicle.setLicensePlate(request.getLicensePlate());
        vehicle.setVehicleType(vehicleType);
        vehicle.setInsuranceNumber(request.getInsuranceNumber());
        vehicle.setInsuranceExpiry(request.getInsuranceExpiry());
        if (request.getStatus() != null) {
            vehicle.setStatus(request.getStatus());
        }

        Vehicle updatedVehicle = vehicleRepository.save(vehicle);
        return vehicleMapper.toResponseDTO(updatedVehicle);
    }

    @Override
    @Transactional
    public void deleteVehicle(Integer id) {
        if (!vehicleRepository.existsById(id)) {
            throw new NotFoundException("Vehicle not found with id: " + id);
        }
        vehicleRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleStatsDTO getVehicleStats(Integer routeId) {
        long total;
        long operational;
        long maintenance;
        long inactive;

        if (routeId != null) {
            // Filter by route
            total = vehicleRepository.countByRouteId(routeId);
            operational = vehicleRepository.countByRouteIdAndStatus(routeId, "Operational");
            maintenance = vehicleRepository.countByRouteIdAndStatus(routeId, "MAINTENANCE");
            inactive = vehicleRepository.countByRouteIdAndStatus(routeId, "INACTIVE");
        } else {
            // All vehicles
            total = vehicleRepository.count();
            operational = vehicleRepository.countByStatusIgnoreCase("Operational");
            maintenance = vehicleRepository.countByStatusIgnoreCase("MAINTENANCE");
            inactive = vehicleRepository.countByStatusIgnoreCase("INACTIVE");
        }

        return VehicleStatsDTO.builder()
                .total(total)
                .operational(operational)
                .maintenance(maintenance)
                .inactive(inactive)
                .build();
    }
}
