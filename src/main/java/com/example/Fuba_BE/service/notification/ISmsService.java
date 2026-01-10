package com.example.Fuba_BE.service.notification;

public interface ISmsService {
    
    /**
     * Send password reset SMS with OTP
     * @param phoneNumber recipient phone number
     * @param otp one-time password for reset
     */
    void sendPasswordResetSms(String phoneNumber, String otp);
    
    /**
     * Send verification SMS with OTP
     * @param phoneNumber recipient phone number
     * @param otp one-time password for verification
     */
    void sendVerificationSms(String phoneNumber, String otp);
}
