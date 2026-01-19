package com.example.Fuba_BE.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.Fuba_BE.domain.entity.Driver;
import com.example.Fuba_BE.domain.entity.DriverRouteAssignment;
import com.example.Fuba_BE.domain.entity.Role;
import com.example.Fuba_BE.domain.entity.Route;
import com.example.Fuba_BE.domain.entity.User;
import com.example.Fuba_BE.exception.BadRequestException;
import com.example.Fuba_BE.exception.NotFoundException;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.repository.DriverRepository;
import com.example.Fuba_BE.repository.DriverRouteAssignmentRepository;
import com.example.Fuba_BE.repository.RoleRepository;
import com.example.Fuba_BE.repository.RouteRepository;
import com.example.Fuba_BE.repository.UserRepository;
import com.example.Fuba_BE.service.CloudinaryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Endpoint để tạo nhanh 50 tài xế với dữ liệu mẫu
 * Chỉ cần upload 1 ảnh avatar, hệ thống sẽ tự tạo 50 tài xế và gắn vào 6 tuyến
 * có sẵn
 */
@RestController
@RequestMapping("/admin/seed")
@RequiredArgsConstructor
@Slf4j
public class DriverSeederController {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final RoleRepository roleRepository;
    private final RouteRepository routeRepository;
    private final DriverRouteAssignmentRepository assignmentRepository;
    private final CloudinaryService cloudinaryService;
    private final PasswordEncoder passwordEncoder;

    // Vietnamese name components
    private static final String[] HO = { "Nguyễn", "Lê", "Trần", "Huỳnh", "Bùi" };
    private static final String[] TEN_DEM = { "Thanh", "Mai", "Văn", "Công", "Hoàng" };
    private static final String[] TEN = { "Tuấn", "Dương", "Lộc", "Chi", "My", "Hải", "Huy", "Vy", "Tâm" };

    private static final String DEFAULT_PASSWORD = "Admin@123";
    private static final LocalDate LICENSE_EXPIRY = LocalDate.of(2050, 1, 1);
    private static final String PHONE_PREFIX = "01354567";
    private static final int DRIVER_COUNT = 50;

    /**
     * Tạo nhanh 50 tài xế
     * POST /admin/seed/drivers
     * Form-data: avatar (file)
     */
    @PostMapping(value = "/drivers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> seedDrivers(
            @RequestParam("avatar") MultipartFile avatarFile) {

        log.info("🚀 Starting driver seeding process...");

        if (avatarFile == null || avatarFile.isEmpty()) {
            throw new BadRequestException("Avatar file is required");
        }

        // 1. Upload avatar một lần duy nhất
        String avatarUrl;
        try {
            log.info("📸 Uploading shared avatar to Cloudinary...");
            avatarUrl = cloudinaryService.uploadImage(avatarFile);
            log.info("✅ Avatar uploaded: {}", avatarUrl);
        } catch (Exception e) {
            log.error("❌ Failed to upload avatar: {}", e.getMessage());
            throw new BadRequestException("Failed to upload avatar: " + e.getMessage());
        }

        // 2. Lấy DRIVER role
        Role driverRole = roleRepository.findByRoleName("DRIVER")
                .orElseThrow(() -> new NotFoundException("DRIVER role not found"));

        // 3. Lấy 6 tuyến có sẵn (Active)
        List<Route> routes = routeRepository.findByStatus("Active");
        if (routes.isEmpty()) {
            throw new BadRequestException("No active routes found. Please create routes first.");
        }
        // Lấy tối đa 6 tuyến
        List<Route> selectedRoutes = routes.size() > 6 ? routes.subList(0, 6) : routes;
        log.info("📍 Found {} routes for assignment", selectedRoutes.size());

        // 4. Tạo 50 tài xế
        Random random = new Random();
        List<Driver> createdDrivers = new ArrayList<>();
        List<DriverRouteAssignment> createdAssignments = new ArrayList<>();
        String encodedPassword = passwordEncoder.encode(DEFAULT_PASSWORD);

        for (int i = 1; i <= DRIVER_COUNT; i++) {
            // Generate random name
            String ho = HO[random.nextInt(HO.length)];
            String tenDem = TEN_DEM[random.nextInt(TEN_DEM.length)];
            String ten = TEN[random.nextInt(TEN.length)];
            String fullName = ho + " " + tenDem + " " + ten;

            // Generate unique identifiers
            String phoneNumber = PHONE_PREFIX + String.format("%02d", i);
            String email = String.format("taixe%02d@fubabus.com", i);
            String driverLicense = String.format("DL%06d", 100000 + i);

            // Check if email already exists (skip if exists)
            if (userRepository.existsByEmail(email)) {
                log.warn("⚠️ Skipping - Email already exists: {}", email);
                continue;
            }

            // Check if phone already exists
            if (userRepository.existsByPhoneNumber(phoneNumber)) {
                log.warn("⚠️ Skipping - Phone already exists: {}", phoneNumber);
                continue;
            }

            // Create User
            User user = User.builder()
                    .fullName(fullName)
                    .email(email)
                    .phoneNumber(phoneNumber)
                    .password(encodedPassword)
                    .role(driverRole)
                    .status("Active")
                    .avt(avatarUrl)
                    .build();

            user = userRepository.save(user);

            // Create Driver profile
            // Random date of birth between 1970-1995
            LocalDate dob = LocalDate.of(1970 + random.nextInt(25), 1 + random.nextInt(12), 1 + random.nextInt(28));

            Driver driver = Driver.builder()
                    .user(user)
                    .driverLicense(driverLicense)
                    .licenseExpiry(LICENSE_EXPIRY)
                    .dateOfBirth(dob)
                    .salary(new BigDecimal("15000000")) // 15 triệu VND
                    .build();

            driver = driverRepository.save(driver);
            createdDrivers.add(driver);

            // Assign to route (round-robin)
            Route assignedRoute = selectedRoutes.get((i - 1) % selectedRoutes.size());

            // Random preferred role
            String preferredRole = random.nextBoolean() ? "Main" : "SubDriver";

            DriverRouteAssignment assignment = DriverRouteAssignment.builder()
                    .driver(driver)
                    .route(assignedRoute)
                    .preferredRole(preferredRole)
                    .priority((i - 1) / selectedRoutes.size() + 1) // Priority increases as we cycle
                    .startDate(LocalDate.now())
                    .isActive(true)
                    .build();

            assignment = assignmentRepository.save(assignment);
            createdAssignments.add(assignment);

            if (i % 10 == 0) {
                log.info("📊 Progress: {}/{} drivers created", i, DRIVER_COUNT);
            }
        }

        log.info("✅ Driver seeding completed!");

        // Prepare response
        Map<String, Object> result = new HashMap<>();
        result.put("driversCreated", createdDrivers.size());
        result.put("assignmentsCreated", createdAssignments.size());
        result.put("routesUsed", selectedRoutes.size());
        result.put("sharedAvatarUrl", avatarUrl);
        result.put("defaultPassword", DEFAULT_PASSWORD);
        result.put("emailFormat", "taixeXX@fubabus.com");
        result.put("phoneFormat", PHONE_PREFIX + "XX");
        result.put("licenseExpiry", LICENSE_EXPIRY.toString());

        // List created drivers summary
        List<Map<String, String>> driverSummary = new ArrayList<>();
        for (Driver d : createdDrivers) {
            Map<String, String> info = new HashMap<>();
            info.put("id", d.getDriverId().toString());
            info.put("name", d.getUser().getFullName());
            info.put("email", d.getUser().getEmail());
            info.put("phone", d.getUser().getPhoneNumber());
            driverSummary.add(info);
        }
        result.put("drivers", driverSummary);

        return ResponseEntity.ok(ApiResponse.success(
                "Created " + createdDrivers.size() + " drivers and " + createdAssignments.size() + " route assignments",
                result));
    }

    /**
     * Xóa tất cả tài xế seed (dựa trên email pattern taixeXX@fubabus.com)
     * DELETE /admin/seed/drivers
     */
    @PostMapping("/drivers/cleanup")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> cleanupSeededDrivers() {
        log.info("🧹 Starting cleanup of seeded drivers...");

        int deletedAssignments = 0;
        int deletedDrivers = 0;
        int deletedUsers = 0;

        // Find users with seeded email pattern
        List<User> seededUsers = userRepository.findAll().stream()
                .filter(u -> u.getEmail() != null && u.getEmail().matches("taixe\\d{2}@fubabus\\.com"))
                .toList();

        for (User user : seededUsers) {
            // Find driver profile
            driverRepository.findByUserId(user.getUserId()).ifPresent(driver -> {
                // Delete assignments first
                List<DriverRouteAssignment> assignments = assignmentRepository
                        .findByDriverDriverId(driver.getDriverId());
                assignmentRepository.deleteAll(assignments);

                // Delete driver
                driverRepository.delete(driver);
            });

            // Delete user
            userRepository.delete(user);
            deletedUsers++;
        }

        log.info("✅ Cleanup completed: {} users deleted", deletedUsers);

        Map<String, Object> result = new HashMap<>();
        result.put("deletedUsers", deletedUsers);
        result.put("message", "Cleanup completed successfully");

        return ResponseEntity.ok(ApiResponse.success("Cleanup completed", result));
    }
}
