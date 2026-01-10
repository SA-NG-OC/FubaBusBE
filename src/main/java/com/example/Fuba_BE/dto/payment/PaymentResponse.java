package com.example.Fuba_BE.dto.payment;

import com.example.Fuba_BE.dto.Booking.BookingResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for payment initialization
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payment initialization response")
public class PaymentResponse {
    
    @Schema(description = "Payment URL to redirect user", example = "https://test-payment.momo.vn/...")
    private String payUrl;
    
    @Schema(description = "Deeplink for MoMo app", example = "momo://...")
    private String deeplink;
    
    @Schema(description = "QR code URL for payment", example = "https://...")
    private String qrCodeUrl;
    
    @Schema(description = "Order ID (same as booking code)", example = "BK20260109001")
    private String orderId;
    
    @Schema(description = "Request ID for tracking", example = "uuid-xxx-xxx")
    private String requestId;
    
    @Schema(description = "Booking information")
    private BookingResponse booking;
    
    @Schema(description = "Payment status message")
    private String message;
}
