package com.example.Fuba_BE.service.payment;

import com.example.Fuba_BE.dto.payment.MomoIpnRequest;
import com.example.Fuba_BE.dto.payment.MomoPaymentResponse;
import com.example.Fuba_BE.dto.payment.PaymentResponse;

/**
 * Service interface for MoMo payment operations
 */
public interface MomoPaymentService {
    
    /**
     * Create a MoMo payment session for a booking
     * 
     * @param bookingId The booking ID to create payment for
     * @return PaymentResponse containing payUrl and payment details
     */
    PaymentResponse createPayment(Integer bookingId);
    
    /**
     * Handle IPN callback from MoMo after payment is processed
     * 
     * @param ipnRequest The IPN request from MoMo
     * @return true if payment was successful and booking updated
     */
    boolean handleIpnCallback(MomoIpnRequest ipnRequest);
    
    /**
     * Query payment status from MoMo
     * 
     * @param orderId The order ID (booking code)
     * @param requestId The original request ID
     * @return MomoPaymentResponse with current payment status
     */
    MomoPaymentResponse queryPaymentStatus(String orderId, String requestId);
    
    /**
     * Verify MoMo signature for security
     * 
     * @param rawData The raw signature data
     * @param signature The signature to verify
     * @return true if signature is valid
     */
    boolean verifySignature(String rawData, String signature);
}
