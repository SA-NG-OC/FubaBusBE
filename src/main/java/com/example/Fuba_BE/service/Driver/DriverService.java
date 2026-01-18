package com.example.Fuba_BE.service.Driver;

import com.example.Fuba_BE.domain.entity.Driver;
import com.example.Fuba_BE.domain.entity.DriverRouteAssignment;
import com.example.Fuba_BE.domain.entity.Role;
import com.example.Fuba_BE.domain.entity.User;
import com.example.Fuba_BE.dto.Driver.CreateDriverWithAccountRequest;
import com.example.Fuba_BE.dto.Driver.DriverRequestDTO;
import com.example.Fuba_BE.dto.Driver.DriverResponseDTO;
import com.example.Fuba_BE.dto.Driver.DriverSelectionDTO;
import com.example.Fuba_BE.exception.BadRequestException;
import com.example.Fuba_BE.exception.NotFoundException;
import com.example.Fuba_BE.mapper.DriverMapper;
import com.example.Fuba_BE.mapper.SelectionMapper;
import com.example.Fuba_BE.repository.DriverRepository;
import com.example.Fuba_BE.repository.DriverRouteAssignmentRepository;
import com.example.Fuba_BE.repository.RoleRepository;
import com.example.Fuba_BE.repository.UserRepository;
import com.example.Fuba_BE.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DriverService implements IDriverService {

    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DriverRouteAssignmentRepository assignmentRepository;
    private final SelectionMapper selectionMapper;
    private final DriverMapper driverMapper;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryService cloudinaryService;

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
    public DriverResponseDTO createDriverWithAccount(CreateDriverWithAccountRequest request, MultipartFile avatarFile) {
        log.info("Creating driver with account - Email: {}, Has avatar: {}", request.getEmail(),
                avatarFile != null && !avatarFile.isEmpty());

        String avatarUrl = null;

        try {
            // Check if email already exists
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BadRequestException("Email already exists: " + request.getEmail());
            }

            // Check if phone already exists
            if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new BadRequestException("Phone number already exists: " + request.getPhoneNumber());
            }

            // Upload avatar if provided
            if (avatarFile != null && !avatarFile.isEmpty()) {
                log.info("Uploading driver avatar to Cloudinary");
                try {
                    avatarUrl = cloudinaryService.uploadImage(avatarFile);
                    log.info("Avatar uploaded successfully: {}", avatarUrl);
                } catch (Exception uploadException) {
                    log.error("Avatar upload failed: {}", uploadException.getMessage());
                    throw new BadRequestException("Failed to upload avatar: " + uploadException.getMessage());
                }
            }

            // Get DRIVER role
            Role driverRole = roleRepository.findByRoleName("DRIVER")
                    .orElseThrow(() -> new NotFoundException("DRIVER role not found"));

            // Create user account
            User user = User.builder()
                    .fullName(request.getFullName())
                    .email(request.getEmail())
                    .phoneNumber(request.getPhoneNumber())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(driverRole)
                    .status("Active")
                    .avt(avatarUrl)
                    .build();

            user = userRepository.save(user);
            log.info("Created user account with ID: {}", user.getUserId());

            // Create driver profile
            Driver driver = Driver.builder()
                    .user(user)
                    .driverLicense(request.getDriverLicense())
                    .licenseExpiry(request.getLicenseExpiry())
                    .dateOfBirth(request.getDateOfBirth())
                    .salary(request.getSalary())
                    .build();

            driver = driverRepository.save(driver);
            log.info("Created driver with ID: {}", driver.getDriverId());

            return mapToResponseDTO(driver);

        } catch (Exception e) {
            // If transaction fails and avatar was uploaded, delete it
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                log.warn("Transaction failed, deleting uploaded avatar: {}", avatarUrl);
                cloudinaryService.deleteImageByUrl(avatarUrl);
            }
            throw e;
        }
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
