package com.example.Fuba_BE.service.notification;

public interface IEmailService {
    
    /**
     * Send password reset email with token
     * @param toEmail recipient email address
     * @param resetToken password reset token
     */
    void sendPasswordResetEmail(String toEmail, String resetToken);
    
    /**
     * Send welcome email after registration
     * @param toEmail recipient email address
     * @param fullName user's full name
     */
    void sendWelcomeEmail(String toEmail, String fullName);
    
    /**
     * Send email verification link
     * @param toEmail recipient email address
     * @param verificationToken email verification token
     */
    void sendEmailVerification(String toEmail, String verificationToken);
}
