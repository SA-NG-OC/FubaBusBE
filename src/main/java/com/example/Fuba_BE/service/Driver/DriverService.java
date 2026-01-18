package com.example.Fuba_BE.service.Driver;

import com.example.Fuba_BE.domain.entity.Driver;
import com.example.Fuba_BE.domain.entity.DriverRouteAssignment;
import com.example.Fuba_BE.domain.entity.User;
import com.example.Fuba_BE.dto.Driver.DriverRequestDTO;
import com.example.Fuba_BE.dto.Driver.DriverResponseDTO;
import com.example.Fuba_BE.dto.Driver.DriverSelectionDTO;
import com.example.Fuba_BE.exception.BadRequestException;
import com.example.Fuba_BE.exception.NotFoundException;
import com.example.Fuba_BE.mapper.DriverMapper;
import com.example.Fuba_BE.mapper.SelectionMapper;
import com.example.Fuba_BE.repository.DriverRepository;
import com.example.Fuba_BE.repository.DriverRouteAssignmentRepository;
import com.example.Fuba_BE.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DriverService implements IDriverService {

    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final DriverRouteAssignmentRepository assignmentRepository;
    private final SelectionMapper selectionMapper;
    private final DriverMapper driverMapper;

    @Override
    public List<DriverSelectionDTO> getAllDriversForSelection() {
        List<Driver> drivers = driverRepository.findAllWithUserAndRoleDriver();
        return drivers.stream().map(selectionMapper::toDriverSelectionDTO).toList();
    }

    @Override
    public Page<DriverResponseDTO> getAllDrivers(String keyword, Pageable pageable) {
        log.debug("Fetching drivers with keyword: {}, page: {}", keyword, pageable.getPageNumber());

        Page<Driver> drivers = driverRepository.findAllWithUserAndKeyword(keyword, pageable);
        return drivers.map(this::mapToResponseDTO);
    }

    @Override
    public DriverResponseDTO getDriverById(Integer id) {
        log.info("Fetching driver by ID: {}", id);

        Driver driver = driverRepository.findByIdWithUser(id)
                .orElseThrow(() -> new NotFoundException("Driver not found with ID: " + id));

        return mapToResponseDTO(driver);
    }

    @Override
    @Transactional
    public DriverResponseDTO createDriver(DriverRequestDTO request) {
        log.info("Creating driver with license: {}", request.getDriverLicense());

        // Validate user exists
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + request.getUserId()));

        // Check if user already has driver profile
        if (driverRepository.findByUserId(request.getUserId()).isPresent()) {
            throw new BadRequestException("User already has a driver profile");
        }

        // Create driver
        Driver driver = new Driver();
        driver.setUser(user);
        driver.setDriverLicense(request.getDriverLicense());
        driver.setLicenseExpiry(request.getLicenseExpiry());
        driver.setDateOfBirth(request.getDateOfBirth());
        driver.setSalary(request.getSalary());

        driver = driverRepository.save(driver);
        log.info("Created driver with ID: {}", driver.getDriverId());

        return mapToResponseDTO(driver);
    }

    @Override
    @Transactional
    public DriverResponseDTO updateDriver(Integer id, DriverRequestDTO request) {
        log.info("Updating driver ID: {}", id);

        Driver driver = driverRepository.findByIdWithUser(id)
                .orElseThrow(() -> new NotFoundException("Driver not found with ID: " + id));

        // Update fields
        driver.setDriverLicense(request.getDriverLicense());
        driver.setLicenseExpiry(request.getLicenseExpiry());
        driver.setDateOfBirth(request.getDateOfBirth());
        driver.setSalary(request.getSalary());

        driver = driverRepository.save(driver);
        log.info("Updated driver ID: {}", id);

        return mapToResponseDTO(driver);
    }

    @Override
    @Transactional
    public void deleteDriver(Integer id) {
        log.info("Deleting driver ID: {}", id);

        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Driver not found with ID: " + id));

        driverRepository.delete(driver);
        log.info("Deleted driver ID: {}", id);
    }

    private DriverResponseDTO mapToResponseDTO(Driver driver) {
        // Fetch active route assignments
        List<DriverRouteAssignment> assignments = assignmentRepository.findActiveByDriverId(driver.getDriverId());

        // Use mapper to convert
        return driverMapper.toResponseDTO(driver, assignments);
    }
}
