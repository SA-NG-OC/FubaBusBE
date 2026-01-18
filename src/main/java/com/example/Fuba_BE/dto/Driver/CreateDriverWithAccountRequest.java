package com.example.Fuba_BE.dto.Driver;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating driver with user account and avatar in one
 * transaction
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDriverWithAccountRequest {

    // User Account fields
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
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", message = "Password must contain at least one uppercase, one lowercase, and one digit")
    private String password;

    // Driver-specific fields
    @NotBlank(message = "Driver license is required")
    private String driverLicense;

    @NotNull(message = "License expiry date is required")
    private LocalDate licenseExpiry;

    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;

    private BigDecimal salary;

    // Optional avatar URL (if uploaded separately first)
    private String avatarUrl;
}
