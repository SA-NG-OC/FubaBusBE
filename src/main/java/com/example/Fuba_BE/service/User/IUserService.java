package com.example.Fuba_BE.service.User;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.example.Fuba_BE.dto.User.CreateEmployeeWithAccountRequest;
import com.example.Fuba_BE.dto.User.CreateUserByAdminRequest;
import com.example.Fuba_BE.dto.User.ProfileResponseDTO;
import com.example.Fuba_BE.dto.User.UpdatePasswordRequest;
import com.example.Fuba_BE.dto.User.UpdateProfileRequest;
import com.example.Fuba_BE.dto.User.UserResponseDTO;

public interface IUserService {

    /**
     * Admin creates user with specific role
     */
    UserResponseDTO createUserByAdmin(CreateUserByAdminRequest request);

    /**
     * Admin creates employee (STAFF role) with avatar
     */
    UserResponseDTO createEmployeeWithAccount(CreateEmployeeWithAccountRequest request, MultipartFile avatarFile);

    /**
     * Get all users with pagination
     */
    Page<UserResponseDTO> getAllUsers(Pageable pageable);

    /**
     * Get user by ID
     */
    UserResponseDTO getUserById(Integer userId);

    /**
     * Get users by role
     */
    List<UserResponseDTO> getUsersByRole(Integer roleId);

    /**
     * Update user status
     */
    UserResponseDTO updateUserStatus(Integer userId, String status);

    /**
     * Delete user (soft delete by changing status)
     */
    void deleteUser(Integer userId);

    // --- Profile Management ---

    /**
     * Get own profile (authenticated user)
     */
    ProfileResponseDTO getMyProfile(Integer userId);

    /**
     * Update own profile (authenticated user)
     */
    ProfileResponseDTO updateMyProfile(Integer userId, UpdateProfileRequest request);

    /**
     * Change own password (authenticated user)
     */
    void updateMyPassword(Integer userId, UpdatePasswordRequest request);

    /**
     * Get employee profile by ID (ADMIN/STAFF only)
     */
    ProfileResponseDTO getEmployeeProfile(Integer employeeId);

    /**
     * Update employee profile by ID (ADMIN/STAFF only)
     */
    ProfileResponseDTO updateEmployeeProfile(Integer employeeId, UpdateProfileRequest request);

    // --- Avatar Management ---

    /**
     * Upload avatar for user
     */
    ProfileResponseDTO uploadAvatar(Integer userId, MultipartFile file);

    /**
     * Delete avatar for user (revert to default)
     */
    ProfileResponseDTO deleteAvatar(Integer userId);

    // --- Customer Management (Admin) ---

    /**
     * Get customers (role USER) with search and filter
     */
    Page<UserResponseDTO> getCustomers(String search, String status, Pageable pageable);

    Page<UserResponseDTO> getAllUsers(Integer roleId, String keyword, Pageable pageable);
}
