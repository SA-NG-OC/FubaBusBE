package com.example.Fuba_BE.service.User;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.Fuba_BE.dto.User.CreateUserByAdminRequest;
import com.example.Fuba_BE.dto.User.UserResponseDTO;

public interface IUserService {

    /**
     * Admin creates user with specific role
     */
    UserResponseDTO createUserByAdmin(CreateUserByAdminRequest request);

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
}
