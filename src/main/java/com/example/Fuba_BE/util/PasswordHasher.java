package com.example.Fuba_BE.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHasher {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // Hash password for: Admin@123
        String password = "Admin@123";
        String hashedPassword = encoder.encode(password);
        
        System.out.println("========================================");
        System.out.println("Password: " + password);
        System.out.println("BCrypt Hash: " + hashedPassword);
        System.out.println("========================================");
        
        // Verify it works
        boolean matches = encoder.matches(password, hashedPassword);
        System.out.println("Verification: " + (matches ? "SUCCESS" : "FAILED"));
    }
}
