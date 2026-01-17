package com.example.Fuba_BE.service.Driver;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.Fuba_BE.dto.Driver.DriverRequestDTO;
import com.example.Fuba_BE.dto.Driver.DriverResponseDTO;
import com.example.Fuba_BE.dto.Driver.DriverSelectionDTO;

public interface IDriverService {
    List<DriverSelectionDTO> getAllDriversForSelection();

    Page<DriverResponseDTO> getAllDrivers(String keyword, Pageable pageable);

    DriverResponseDTO getDriverById(Integer id);

    DriverResponseDTO createDriver(DriverRequestDTO request);

    DriverResponseDTO updateDriver(Integer id, DriverRequestDTO request);

    void deleteDriver(Integer id);
}
