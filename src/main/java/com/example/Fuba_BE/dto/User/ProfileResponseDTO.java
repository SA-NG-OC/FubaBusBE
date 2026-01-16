package com.example.Fuba_BE.dto.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponseDTO {
    private Integer userId;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String status;

    // Role information
    private Integer roleId;
    private String roleName;
    private String roleDescription;
    private String avatarUrl;
}
