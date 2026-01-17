package com.example.Fuba_BE.dto.User;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for user information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {

    private Integer userId;
    private String fullName;
    private String email;
    private String phoneNumber;

    // Role information
    private Integer roleId;
    private String roleName;
    private String roleDescription;

    private String status;
    private Boolean emailVerified;
    private LocalDateTime lastLoginAt;
    private String avatarUrl;
}
