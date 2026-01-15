package com.example.Fuba_BE.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.Fuba_BE.dto.User.CreateUserByAdminRequest;
import com.example.Fuba_BE.dto.User.ProfileResponseDTO;
import com.example.Fuba_BE.dto.User.UpdatePasswordRequest;
import com.example.Fuba_BE.dto.User.UpdateProfileRequest;
import com.example.Fuba_BE.dto.User.UserResponseDTO;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.security.UserPrincipal;
import com.example.Fuba_BE.service.User.IUserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for user management (admin operations & profile management)
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final IUserService userService;

    // --- Admin User Management ---

    /**
     * Admin creates user with specific role
     * Only ADMIN can create users
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponseDTO>> createUser(
            @Valid @RequestBody CreateUserByAdminRequest request
    ) {
        log.info("📥 Request to create user with email: {}", request.getEmail());
        UserResponseDTO response = userService.createUserByAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created successfully", response));
    }

    /**
     * Get all users with pagination
     * ADMIN and STAFF can view all users
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<UserResponseDTO>>> getAllUsers(
            @PageableDefault(
                    page = 0,
                    size = 20,
                    sort = "userId",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        log.debug("📥 Request to get all users");
        Page<UserResponseDTO> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }

    /**
     * Get user by ID
     * ADMIN and STAFF can view user details
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getUserById(
            @PathVariable Integer id
    ) {
        log.info("📥 Request to get user by ID: {}", id);
        UserResponseDTO user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", user));
    }

    /**
     * Get users by role
     * ADMIN and STAFF can view users by role
     */
    @GetMapping("/role/{roleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<UserResponseDTO>>> getUsersByRole(
            @PathVariable Integer roleId
    ) {
        log.info("📥 Request to get users by role ID: {}", roleId);
        List<UserResponseDTO> users = userService.getUsersByRole(roleId);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }

    /**
     * Update user status
     * Only ADMIN can update user status
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateUserStatus(
            @PathVariable Integer id,
            @RequestParam String status
    ) {
        log.info("📥 Request to update status for user ID: {} to {}", id, status);
        UserResponseDTO user = userService.updateUserStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("User status updated successfully", user));
    }

    /**
     * Delete user (soft delete)
     * Only ADMIN can delete users
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Integer id
    ) {
        log.info("📥 Request to delete user ID: {}", id);
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }

    // --- Profile Management (Self) ---

    /**
     * Get own profile
     * Any authenticated user can view their own profile
     */
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProfileResponseDTO>> getMyProfile(
            Authentication authentication
    ) {
        Integer userId = extractUserId(authentication);
        log.info("📥 User {} requesting own profile", userId);
        
        ProfileResponseDTO profile = userService.getMyProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", profile));
    }

    /**
     * Update own profile
     * Any authenticated user can update their own profile
     */
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProfileResponseDTO>> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication
    ) {
        Integer userId = extractUserId(authentication);
        log.info("📥 User {} updating own profile", userId);
        
        ProfileResponseDTO profile = userService.updateMyProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", profile));
    }

    /**
     * Change own password
     * Any authenticated user can change their own password
     */
    @PutMapping("/profile/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> updateMyPassword(
            @Valid @RequestBody UpdatePasswordRequest request,
            Authentication authentication
    ) {
        Integer userId = extractUserId(authentication);
        log.info("📥 User {} changing password", userId);
        
        userService.updateMyPassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Password updated successfully", null));
    }

    // --- Employee Profile Management (Admin/Staff) ---

    /**
     * Get employee profile by ID
     * ADMIN and STAFF can view employee profiles
     */
    @GetMapping("/{id}/profile")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<ProfileResponseDTO>> getEmployeeProfile(
            @PathVariable Integer id
    ) {
        log.info("📥 Admin/Staff requesting employee profile for ID: {}", id);
        ProfileResponseDTO profile = userService.getEmployeeProfile(id);
        return ResponseEntity.ok(ApiResponse.success("Employee profile retrieved successfully", profile));
    }

    /**
     * Update employee profile by ID
     * ADMIN and STAFF can update employee profiles
     */
    @PutMapping("/{id}/profile")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<ProfileResponseDTO>> updateEmployeeProfile(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        log.info("📥 Admin/Staff updating employee profile for ID: {}", id);
        ProfileResponseDTO profile = userService.updateEmployeeProfile(id, request);
        return ResponseEntity.ok(ApiResponse.success("Employee profile updated successfully", profile));
    }

    // --- Helper Methods ---

    /**
     * Extract user ID from authentication token
     */
    private Integer extractUserId(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return userPrincipal.getUserId();
    }
}
