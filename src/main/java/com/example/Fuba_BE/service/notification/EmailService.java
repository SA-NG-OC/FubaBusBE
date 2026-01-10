package com.example.Fuba_BE.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Email service implementation using Spring Boot JavaMailSender
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService implements IEmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${spring.mail.username}")
    private String smtpUsername;

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("FutaBus - Password Reset Request");
            message.setText(String.format("""
                    Hello,
                    
                    You have requested to reset your password for your FutaBus account.
                    
                    Please use the following link to reset your password:
                    http://localhost:5230/auth/reset-password?token=%s
                    
                    This link will expire in 1 hour.
                    
                    If you did not request this, please ignore this email.
                    
                    Best regards,
                    FutaBus Team
                    """, resetToken));

            mailSender.send(message);
            log.info("Password reset email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send email. Please check SMTP configuration.", e);
        }
    }

    @Override
    public void sendWelcomeEmail(String toEmail, String fullName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Welcome to FutaBus!");
            message.setText(String.format("""
                    Hello %s,
                    
                    Welcome to FutaBus! Your account has been successfully created.
                    
                    You can now:
                    - Search for bus routes
                    - Book tickets online
                    - Manage your bookings
                    - View your travel history
                    
                    Thank you for choosing FutaBus for your travel needs.
                    
                    Best regards,
                    FutaBus Team
                    """, fullName));

            mailSender.send(message);
            log.info("Welcome email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", toEmail, e);
            // Don't throw exception for welcome emails - non-critical
        }
    }

    @Override
    public void sendEmailVerification(String toEmail, String verificationToken) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("FutaBus - Verify Your Email");
            message.setText(String.format("""
                    Hello,
                    
                    Thank you for registering with FutaBus!
                    
                    Please verify your email address by clicking the link below:
                    http://localhost:5230/auth/verify-email?token=%s
                    
                    This link will expire in 24 hours.
                    
                    Best regards,
                    FutaBus Team
                    """, verificationToken));

            mailSender.send(message);
            log.info("Email verification sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", toEmail, e);
        }
    }
}

