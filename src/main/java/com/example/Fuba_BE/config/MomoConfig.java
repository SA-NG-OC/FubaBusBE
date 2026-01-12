package com.example.Fuba_BE.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for MoMo payment gateway.
 * Properties are loaded from application.properties with prefix "momo"
 */
@Configuration
@ConfigurationProperties(prefix = "momo")
@Data
public class MomoConfig {
    
    /**
     * Partner code provided by MoMo
     */
    private String partnerCode;
    
    /**
     * Access key for API authentication
     */
    private String accessKey;
    
    /**
     * Secret key for HMAC signature
     */
    private String secretKey;
    
    /**
     * MoMo API endpoint (sandbox or production)
     */
    private String endpoint;
    
    /**
     * URL to redirect user after payment
     */
    private String redirectUrl;
    
    /**
     * IPN (Instant Payment Notification) URL for server-to-server callback
     */
    private String ipnUrl;
    
    /**
     * Request type for one-time payment
     */
    private String requestType = "captureWallet";
}
