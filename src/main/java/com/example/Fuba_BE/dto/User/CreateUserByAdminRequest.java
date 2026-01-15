package com.example.Fuba_BE.dto.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for admin to create user account with specific role
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserByAdminRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(\\+84|0)[0-9]{9,10}$", message = "Phone number must be valid Vietnamese format")
    private String phoneNumber;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", 
             message = "Password must contain at least one uppercase, one lowercase, and one digit")
    private String password;

    @NotNull(message = "Role ID is required")
    private Integer roleId;

    /**
     * Optional status (default: Active)
     * Valid values: Active, Inactive, Suspended
     */
    @Pattern(regexp = "^(Active|Inactive|Suspended)$", 
             message = "Status must be one of: Active, Inactive, Suspended")
    private String status;
}
