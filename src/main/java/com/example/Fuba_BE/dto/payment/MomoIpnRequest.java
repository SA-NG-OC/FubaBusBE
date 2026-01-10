package com.example.Fuba_BE.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * IPN (Instant Payment Notification) request from MoMo
 * Sent to ipnUrl after payment is processed
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MomoIpnRequest {
    private String partnerCode;
    private String orderId;
    private String requestId;
    private Long amount;
    private String orderInfo;
    private String orderType;
    private Long transId;
    private Integer resultCode;
    private String message;
    private String payType;
    private Long responseTime;
    private String extraData;
    private String signature;
}
