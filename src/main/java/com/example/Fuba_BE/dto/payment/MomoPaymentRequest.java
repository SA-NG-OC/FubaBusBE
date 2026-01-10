package com.example.Fuba_BE.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for MoMo payment creation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MomoPaymentRequest {
    private String partnerCode;
    private String accessKey;
    private String requestId;
    private String amount;
    private String orderId;
    private String orderInfo;
    private String redirectUrl;
    private String ipnUrl;
    private String requestType;
    private String extraData;
    private String lang;
    private String signature;
}
