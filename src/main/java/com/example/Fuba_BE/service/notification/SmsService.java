package com.example.Fuba_BE.service.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Stub implementation for SMS service
 * TODO: Integrate with actual SMS provider (Twilio, AWS SNS, etc.)
 */
@Slf4j
@Service
public class SmsService implements ISmsService {

    @Override
    public void sendPasswordResetSms(String phoneNumber, String otp) {
        log.info("Sending password reset SMS to: {} with OTP: {}", phoneNumber, otp);
        // TODO: Implement actual SMS sending logic
        // Example: Use Twilio API or other SMS gateway
    }

    @Override
    public void sendVerificationSms(String phoneNumber, String otp) {
        log.info("Sending verification SMS to: {} with OTP: {}", phoneNumber, otp);
        // TODO: Implement actual SMS sending logic
    }
}
