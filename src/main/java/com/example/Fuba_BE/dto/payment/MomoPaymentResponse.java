package com.example.Fuba_BE.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response from MoMo payment API
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MomoPaymentResponse {
    private String partnerCode;
    private String requestId;
    private String orderId;
    private Long amount;
    private Long responseTime;
    private String message;
    private Integer resultCode;
    private String payUrl;
    private String deeplink;
    private String qrCodeUrl;
    private String deeplinkMiniApp;
}
