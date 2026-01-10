package com.example.Fuba_BE.dto.Auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Email or phone number is required")
    private String emailOrPhone;

    @NotBlank(message = "Password is required")
    private String password;
}
