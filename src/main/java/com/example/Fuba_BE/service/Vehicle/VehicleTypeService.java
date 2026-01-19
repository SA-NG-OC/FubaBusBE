package com.example.Fuba_BE.service.Vehicle;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Fuba_BE.config.CacheConfig;
import com.example.Fuba_BE.domain.entity.VehicleType;
import com.example.Fuba_BE.dto.Vehicle.VehicleTypeRequestDTO;
import com.example.Fuba_BE.dto.Vehicle.VehicleTypeResponseDTO;
import com.example.Fuba_BE.exception.NotFoundException;
import com.example.Fuba_BE.repository.VehicleTypeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class VehicleTypeService implements IVehicleTypeService {

    private final VehicleTypeRepository vehicleTypeRepository;

    @Override
    public Page<VehicleTypeResponseDTO> getAllVehicleTypes(Pageable pageable) {
        log.debug("📥 Fetching all vehicle types with pagination");
        Page<VehicleType> vehicleTypes = vehicleTypeRepository.findAll(pageable);
        return vehicleTypes.map(this::mapToResponseDTO);
    }

    @Override
    @Cacheable(value = CacheConfig.CACHE_VEHICLE_TYPES_ALL)
    public List<VehicleTypeResponseDTO> getAllVehicleTypesForSelection() {
        log.debug("📥 Cache MISS - Fetching all vehicle types for selection from database");
        List<VehicleType> vehicleTypes = vehicleTypeRepository.findAll();
        return vehicleTypes.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = CacheConfig.CACHE_VEHICLE_TYPES, key = "#id")
    public VehicleTypeResponseDTO getVehicleTypeById(Integer id) {
        log.debug("📥 Cache MISS - Fetching vehicle type by ID: {}", id);
        VehicleType vehicleType = vehicleTypeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Vehicle type not found with ID: " + id));
        return mapToResponseDTO(vehicleType);
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_VEHICLE_TYPES_ALL, allEntries = true)
    public VehicleTypeResponseDTO createVehicleType(VehicleTypeRequestDTO request) {
        log.info("📥 Creating new vehicle type: {}", request.getTypeName());

        VehicleType vehicleType = VehicleType.builder()
                .typeName(request.getTypeName())
                .totalSeats(request.getTotalSeats())
                .numberOfFloors(request.getNumberOfFloors() != null ? request.getNumberOfFloors() : 1)
                .description(request.getDescription())
                .build();

        VehicleType savedVehicleType = vehicleTypeRepository.save(vehicleType);
        log.info("✅ Vehicle type created successfully with ID: {}", savedVehicleType.getTypeId());

        return mapToResponseDTO(savedVehicleType);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.CACHE_VEHICLE_TYPES, key = "#id"),
            @CacheEvict(value = CacheConfig.CACHE_VEHICLE_TYPES_ALL, allEntries = true)
    })
    public VehicleTypeResponseDTO updateVehicleType(Integer id, VehicleTypeRequestDTO request) {
        log.info("📥 Updating vehicle type ID: {}", id);

        VehicleType vehicleType = vehicleTypeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Vehicle type not found with ID: " + id));

        vehicleType.setTypeName(request.getTypeName());
        vehicleType.setTotalSeats(request.getTotalSeats());
        vehicleType.setNumberOfFloors(request.getNumberOfFloors() != null ? request.getNumberOfFloors() : 1);
        vehicleType.setDescription(request.getDescription());

        VehicleType updatedVehicleType = vehicleTypeRepository.save(vehicleType);
        log.info("✅ Vehicle type updated successfully");

        return mapToResponseDTO(updatedVehicleType);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.CACHE_VEHICLE_TYPES, key = "#id"),
            @CacheEvict(value = CacheConfig.CACHE_VEHICLE_TYPES_ALL, allEntries = true)
    })
    public void deleteVehicleType(Integer id) {
        log.info("📥 Deleting vehicle type ID: {}", id);

        if (!vehicleTypeRepository.existsById(id)) {
            throw new NotFoundException("Vehicle type not found with ID: " + id);
        }

        vehicleTypeRepository.deleteById(id);
        log.info("✅ Vehicle type deleted successfully");
    }

    // Helper method to map entity to DTO
    private VehicleTypeResponseDTO mapToResponseDTO(VehicleType vehicleType) {
        return VehicleTypeResponseDTO.builder()
                .typeId(vehicleType.getTypeId())
                .typeName(vehicleType.getTypeName())
                .totalSeats(vehicleType.getTotalSeats())
                .numberOfFloors(vehicleType.getNumberOfFloors())
                .description(vehicleType.getDescription())
                .build();
    }
}
