package com.example.Fuba_BE.service.User;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Fuba_BE.domain.entity.Role;
import com.example.Fuba_BE.domain.entity.User;
import com.example.Fuba_BE.dto.User.CreateUserByAdminRequest;
import com.example.Fuba_BE.dto.User.UserResponseDTO;
import com.example.Fuba_BE.exception.BadRequestException;
import com.example.Fuba_BE.exception.ResourceNotFoundException;
import com.example.Fuba_BE.mapper.UserMapper;
import com.example.Fuba_BE.repository.RoleRepository;
import com.example.Fuba_BE.repository.UserRepository;

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
    @Transactional(readOnly = true)
    public Page<UserResponseDTO> getAllUsers(Pageable pageable) {
        log.debug("🔍 Fetching all users with pagination");
        Page<User> users = userRepository.findAll(pageable);
        return users.map(userMapper::toResponseDTO);
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
        return userMapper.toResponseDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getUsersByRole(Integer roleId) {
        log.info("🔍 Fetching users by role ID: {}", roleId);

        // Validate role exists
        validateRoleExists(roleId);

        List<User> users = userRepository.findByRoleId(roleId);
        return users.stream()
                .map(userMapper::toResponseDTO)
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
}
