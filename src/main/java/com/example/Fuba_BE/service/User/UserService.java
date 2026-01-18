package com.example.Fuba_BE.service.User;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.Fuba_BE.domain.entity.Role;
import com.example.Fuba_BE.domain.entity.User;
import com.example.Fuba_BE.dto.User.CreateEmployeeWithAccountRequest;
import com.example.Fuba_BE.dto.User.CreateUserByAdminRequest;
import com.example.Fuba_BE.dto.User.ProfileResponseDTO;
import com.example.Fuba_BE.dto.User.UpdatePasswordRequest;
import com.example.Fuba_BE.dto.User.UpdateProfileRequest;
import com.example.Fuba_BE.dto.User.UserResponseDTO;
import com.example.Fuba_BE.exception.BadRequestException;
import com.example.Fuba_BE.exception.ResourceNotFoundException;
import com.example.Fuba_BE.exception.UnauthorizedException;
import com.example.Fuba_BE.mapper.UserMapper;
import com.example.Fuba_BE.repository.RoleRepository;
import com.example.Fuba_BE.repository.UserRepository;
import com.example.Fuba_BE.service.CloudinaryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserService implements IUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryService cloudinaryService;

    @Override
    public UserResponseDTO createUserByAdmin(CreateUserByAdminRequest request) {
        log.info("🚀 Admin creating new user with email: {}", request.getEmail());

        try {
            // Validate role exists
            validateRoleExists(request.getRoleId());

            // Validate email and phone uniqueness
            validateEmailNotExists(request.getEmail());
            validatePhoneNotExists(request.getPhoneNumber());

            // Get role entity
            Role role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> {
                        log.error("❌ Role not found with ID: {}", request.getRoleId());
                        return new ResourceNotFoundException("Role not found with ID: " + request.getRoleId());
                    });

            log.info("✅ Creating user with role: {} ({})", role.getRoleName(), role.getRoleId());

            // Build user entity
            User user = User.builder()
                    .fullName(request.getFullName())
                    .email(request.getEmail())
                    .phoneNumber(request.getPhoneNumber())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(role)
                    .status(request.getStatus() != null ? request.getStatus() : "Active")
                    .emailVerified(false)
                    .failedLoginAttempts(0)
                    .build();

            User savedUser = userRepository.save(user);
            log.info("✅ User created successfully with ID: {} and role: {}", savedUser.getUserId(), role.getRoleName());

            return userMapper.toResponseDTO(savedUser);

        } catch (BadRequestException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("🔥 Critical error creating user: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to create user: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public UserResponseDTO createEmployeeWithAccount(CreateEmployeeWithAccountRequest request,
            MultipartFile avatarFile) {
        log.info("🚀 Admin creating new employee (STAFF) with email: {}, Has avatar: {}", request.getEmail(),
                avatarFile != null && !avatarFile.isEmpty());

        String avatarUrl = null;

        try {
            // Validate email and phone uniqueness
            validateEmailNotExists(request.getEmail());
            validatePhoneNotExists(request.getPhoneNumber());

            // Upload avatar if provided
            if (avatarFile != null && !avatarFile.isEmpty()) {
                log.info("Uploading employee avatar to Cloudinary");
                try {
                    avatarUrl = cloudinaryService.uploadImage(avatarFile);
                    log.info("Avatar uploaded successfully: {}", avatarUrl);
                } catch (Exception uploadException) {
                    log.error("Avatar upload failed: {}", uploadException.getMessage());
                    throw new BadRequestException("Failed to upload avatar: " + uploadException.getMessage());
                }
            }

            // Get STAFF role
            Role staffRole = roleRepository.findByRoleName("STAFF")
                    .orElseThrow(() -> {
                        log.error("❌ STAFF role not found");
                        return new ResourceNotFoundException("STAFF role not found");
                    });

            log.info("✅ Creating employee with STAFF role");

            // Build user entity
            User user = User.builder()
                    .fullName(request.getFullName())
                    .email(request.getEmail())
                    .phoneNumber(request.getPhoneNumber())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(staffRole)
                    .status("Active")
                    .avt(avatarUrl)
                    .emailVerified(false)
                    .failedLoginAttempts(0)
                    .build();

            User savedUser = userRepository.save(user);
            log.info("✅ Employee created successfully with ID: {}", savedUser.getUserId());

            return userMapper.toResponseDTO(savedUser);

        } catch (BadRequestException | ResourceNotFoundException e) {
            // If transaction fails and avatar was uploaded, delete it
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                log.warn("Transaction failed, deleting uploaded avatar: {}", avatarUrl);
                cloudinaryService.deleteImageByUrl(avatarUrl);
            }
            throw e;
        } catch (Exception e) {
            // If transaction fails and avatar was uploaded, delete it
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                log.warn("Transaction failed, deleting uploaded avatar: {}", avatarUrl);
                cloudinaryService.deleteImageByUrl(avatarUrl);
            }
            log.error("🔥 Critical error creating employee: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to create employee: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDTO> getAllUsers(Pageable pageable) {
        log.debug("🔍 Fetching all users with pagination");
        Page<User> users = userRepository.findAll(pageable);
        return users.map(user -> {
            UserResponseDTO dto = userMapper.toResponseDTO(user);
            if (dto.getAvatarUrl() == null || dto.getAvatarUrl().isEmpty()) {
                dto.setAvatarUrl(cloudinaryService.getDefaultAvatarUrl());
            }
            return dto;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Integer userId) {
        log.info("🔍 Fetching user by ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("❌ User not found with ID: {}", userId);
                    return new ResourceNotFoundException("User not found with ID: " + userId);
                });
        UserResponseDTO response = userMapper.toResponseDTO(user);
        if (response.getAvatarUrl() == null || response.getAvatarUrl().isEmpty()) {
            response.setAvatarUrl(cloudinaryService.getDefaultAvatarUrl());
        }
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getUsersByRole(Integer roleId) {
        log.info("🔍 Fetching users by role ID: {}", roleId);

        // Validate role exists
        validateRoleExists(roleId);

        List<User> users = userRepository.findByRoleId(roleId);
        return users.stream()
                .map(user -> {
                    UserResponseDTO dto = userMapper.toResponseDTO(user);
                    if (dto.getAvatarUrl() == null || dto.getAvatarUrl().isEmpty()) {
                        dto.setAvatarUrl(cloudinaryService.getDefaultAvatarUrl());
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public UserResponseDTO updateUserStatus(Integer userId, String status) {
        log.info("🔄 Updating status for user ID: {} to {}", userId, status);

        // Validate status
        if (!status.matches("^(Active|Inactive|Suspended)$")) {
            log.warn("⚠️ Invalid status: {}", status);
            throw new BadRequestException("Status must be one of: Active, Inactive, Suspended");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("❌ User not found with ID: {}", userId);
                    return new ResourceNotFoundException("User not found with ID: " + userId);
                });

        user.setStatus(status);

        User updatedUser = userRepository.save(user);
        log.info("✅ User status updated successfully for ID: {}", userId);

        return userMapper.toResponseDTO(updatedUser);
    }

    @Override
    public void deleteUser(Integer userId) {
        log.info("🗑 Deleting user ID: {}", userId);

        if (!userRepository.existsById(userId)) {
            log.error("❌ Cannot delete - user not found: ID {}", userId);
            throw new ResourceNotFoundException("User not found with ID: " + userId);
        }

        // Soft delete by updating status to Inactive
        User user = userRepository.findById(userId).get();
        user.setStatus("Inactive");
        userRepository.save(user);

        log.info("✅ User soft deleted successfully: ID {}", userId);
    }

    // --- Profile Management ---

    @Override
    @Transactional(readOnly = true)
    public ProfileResponseDTO getMyProfile(Integer userId) {
        log.info("🔍 Fetching profile for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("❌ User not found with ID: {}", userId);
                    return new ResourceNotFoundException("User not found with ID: " + userId);
                });

        ProfileResponseDTO response = userMapper.toProfileResponseDTO(user);
        // Ensure default fallback if avatar is null or empty
        if (response.getAvatarUrl() == null || response.getAvatarUrl().isEmpty()) {
            response.setAvatarUrl(cloudinaryService.getDefaultAvatarUrl());
        }
        return response;
    }

    @Override
    public ProfileResponseDTO updateMyProfile(Integer userId, UpdateProfileRequest request) {
        log.info("🔄 Updating profile for user ID: {}", userId);

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.error("❌ User not found with ID: {}", userId);
                        return new ResourceNotFoundException("User not found with ID: " + userId);
                    });

            // Validate phone number uniqueness if changed
            if (request.getPhoneNumber() != null && !request.getPhoneNumber().equals(user.getPhoneNumber())) {
                validatePhoneNotExists(request.getPhoneNumber());
            }

            // Update basic fields
            user.setFullName(request.getFullName());
            if (request.getPhoneNumber() != null) {
                user.setPhoneNumber(request.getPhoneNumber());
            }

            User updatedUser = userRepository.save(user);
            log.info("✅ Profile updated successfully for user ID: {}", userId);

            ProfileResponseDTO response = userMapper.toProfileResponseDTO(updatedUser);
            // Ensure default fallback if avatar is null or empty
            if (response.getAvatarUrl() == null || response.getAvatarUrl().isEmpty()) {
                response.setAvatarUrl(cloudinaryService.getDefaultAvatarUrl());
            }
            return response;

        } catch (BadRequestException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("🔥 Error updating profile for user ID {}: {}", userId, e.getMessage(), e);
            throw new BadRequestException("Failed to update profile: " + e.getMessage());
        }
    }

    @Override
    public void updateMyPassword(Integer userId, UpdatePasswordRequest request) {
        log.info("🔐 Updating password for user ID: {}", userId);

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.error("❌ User not found with ID: {}", userId);
                        return new ResourceNotFoundException("User not found with ID: " + userId);
                    });

            // Verify current password
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                log.warn("⚠️ Incorrect current password for user ID: {}", userId);
                throw new UnauthorizedException("Current password is incorrect");
            }

            // Verify password confirmation
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                log.warn("⚠️ Password confirmation mismatch for user ID: {}", userId);
                throw new BadRequestException("New password and confirmation do not match");
            }

            // Update password
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);

            log.info("✅ Password updated successfully for user ID: {}", userId);

        } catch (UnauthorizedException | BadRequestException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("🔥 Error updating password for user ID {}: {}", userId, e.getMessage(), e);
            throw new BadRequestException("Failed to update password: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponseDTO getEmployeeProfile(Integer employeeId) {
        log.info("🔍 Admin/Staff fetching employee profile for ID: {}", employeeId);

        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> {
                    log.error("❌ Employee not found with ID: {}", employeeId);
                    return new ResourceNotFoundException("Employee not found with ID: " + employeeId);
                });

        ProfileResponseDTO response = userMapper.toProfileResponseDTO(employee);
        // Ensure default fallback if avatar is null or empty
        if (response.getAvatarUrl() == null || response.getAvatarUrl().isEmpty()) {
            response.setAvatarUrl(cloudinaryService.getDefaultAvatarUrl());
        }
        return response;
    }

    @Override
    public ProfileResponseDTO updateEmployeeProfile(Integer employeeId, UpdateProfileRequest request) {
        log.info("🔄 Admin/Staff updating employee profile for ID: {}", employeeId);

        try {
            User employee = userRepository.findById(employeeId)
                    .orElseThrow(() -> {
                        log.error("❌ Employee not found with ID: {}", employeeId);
                        return new ResourceNotFoundException("Employee not found with ID: " + employeeId);
                    });

            // Validate phone number uniqueness if changed
            if (request.getPhoneNumber() != null && !request.getPhoneNumber().equals(employee.getPhoneNumber())) {
                validatePhoneNotExists(request.getPhoneNumber());
            }

            // Update basic fields
            employee.setFullName(request.getFullName());
            if (request.getPhoneNumber() != null) {
                employee.setPhoneNumber(request.getPhoneNumber());
            }

            User updatedEmployee = userRepository.save(employee);
            log.info("✅ Employee profile updated successfully for ID: {}", employeeId);

            ProfileResponseDTO response = userMapper.toProfileResponseDTO(updatedEmployee);
            // Ensure default fallback if avatar is null or empty
            if (response.getAvatarUrl() == null || response.getAvatarUrl().isEmpty()) {
                response.setAvatarUrl(cloudinaryService.getDefaultAvatarUrl());
            }
            return response;

        } catch (BadRequestException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("🔥 Error updating employee profile for ID {}: {}", employeeId, e.getMessage(), e);
            throw new BadRequestException("Failed to update employee profile: " + e.getMessage());
        }
    }

    // --- Validation Helpers ---

    /**
     * Validate role exists by ID
     */
    private void validateRoleExists(Integer roleId) {
        if (!roleRepository.existsById(roleId)) {
            log.error("❌ Role validation failed: Role ID {} does not exist", roleId);
            throw new ResourceNotFoundException("Role not found with ID: " + roleId);
        }
    }

    /**
     * Validate email does not exist
     */
    private void validateEmailNotExists(String email) {
        if (userRepository.existsByEmail(email)) {
            log.warn("⚠️ Email already exists: {}", email);
            throw new BadRequestException("Email already exists: " + email);
        }
    }

    /**
     * Validate phone number does not exist
     */
    private void validatePhoneNotExists(String phoneNumber) {
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            log.warn("⚠️ Phone number already exists: {}", phoneNumber);
            throw new BadRequestException("Phone number already exists: " + phoneNumber);
        }
    }

    // --- Avatar Management ---

    @Override
    public ProfileResponseDTO uploadAvatar(Integer userId, MultipartFile file) {
        log.info("📸 Uploading avatar for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        try {
            // Delete old avatar if exists (and not default)
            if (user.getAvt() != null && !user.getAvt().isEmpty()
                    && !user.getAvt().equals(cloudinaryService.getDefaultAvatarUrl())) {
                log.info("🗑️ Deleting old avatar: {}", user.getAvt());
                cloudinaryService.deleteImageByUrl(user.getAvt());
            }

            // Upload new avatar
            String newAvatarUrl = cloudinaryService.uploadImage(file);
            user.setAvt(newAvatarUrl);
            userRepository.save(user);

            log.info("✅ Avatar uploaded successfully for user {}: {}", userId, newAvatarUrl);

            ProfileResponseDTO response = userMapper.toProfileResponseDTO(user);
            // Ensure default fallback if null
            if (response.getAvatarUrl() == null || response.getAvatarUrl().isEmpty()) {
                response.setAvatarUrl(cloudinaryService.getDefaultAvatarUrl());
            }
            return response;
        } catch (Exception e) {
            log.error("❌ Failed to upload avatar for user {}", userId, e);
            throw new BadRequestException("Failed to upload avatar: " + e.getMessage());
        }
    }

    @Override
    public ProfileResponseDTO deleteAvatar(Integer userId) {
        log.info("🗑️ Deleting avatar for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Delete from Cloudinary if not default
        if (user.getAvt() != null && !user.getAvt().isEmpty()
                && !user.getAvt().equals(cloudinaryService.getDefaultAvatarUrl())) {
            log.info("🗑️ Deleting avatar from Cloudinary: {}", user.getAvt());
            cloudinaryService.deleteImageByUrl(user.getAvt());
        }

        // Set to default avatar
        user.setAvt(cloudinaryService.getDefaultAvatarUrl());
        userRepository.save(user);

        log.info("✅ Avatar deleted for user {}, reverted to default", userId);

        ProfileResponseDTO response = userMapper.toProfileResponseDTO(user);
        response.setAvatarUrl(cloudinaryService.getDefaultAvatarUrl());
        return response;
    }
}
