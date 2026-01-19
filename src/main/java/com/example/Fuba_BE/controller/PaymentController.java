package com.example.Fuba_BE.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.Fuba_BE.domain.entity.Booking;
import com.example.Fuba_BE.dto.Booking.BookingResponse;
import com.example.Fuba_BE.dto.payment.MomoIpnRequest;
import com.example.Fuba_BE.dto.payment.MomoPaymentResponse;
import com.example.Fuba_BE.dto.payment.PaymentResponse;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.repository.BookingRepository;
import com.example.Fuba_BE.service.Booking.IBookingService;
import com.example.Fuba_BE.service.payment.MomoPaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for MoMo payment operations.
 * Handles payment creation and IPN callback from MoMo.
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "APIs for MoMo payment integration")
@Slf4j
public class PaymentController {

    private final MomoPaymentService momoPaymentService;
    private final BookingRepository bookingRepository;
    private final IBookingService bookingService;

    /**
     * Bypass payment - instantly confirm booking without real payment
     * For testing/demo purposes only
     */
    @PostMapping("/bypass/{bookingId}")
    @Operation(summary = "Bypass payment (Demo/Test only)", description = "Instantly confirm a booking without going through payment gateway. "
            +
            "Returns confirmed booking with tickets.")
    public ResponseEntity<ApiResponse<BookingResponse>> bypassPayment(
            @Parameter(description = "Booking ID", required = true) @PathVariable Integer bookingId) {

        log.info("🔓 BYPASS PAYMENT request for booking ID: {}", bookingId);

        BookingResponse response = bookingService.bypassPayment(bookingId);

        return ResponseEntity.ok(ApiResponse.<BookingResponse>builder()
                .success(true)
                .message("Thanh toán thành công (Bypass mode)")
                .data(response)
                .build());
    }

    /**
     * Create MoMo payment for a booking by booking ID
     */
    @PostMapping("/momo/create/{bookingId}")
    @Operation(summary = "Create MoMo payment by booking ID", description = "Create a MoMo payment session for a booking in Held status. "
            +
            "Returns payment URL to redirect user to MoMo payment page.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid booking status or MoMo error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<ApiResponse<PaymentResponse>> createMomoPayment(
            @Parameter(description = "Booking ID", required = true) @PathVariable Integer bookingId) {

        log.info("Creating MoMo payment for booking ID: {}", bookingId);

        PaymentResponse paymentResponse = momoPaymentService.createPayment(bookingId);

        return ResponseEntity.ok(ApiResponse.<PaymentResponse>builder()
                .success(true)
                .message("Tạo thanh toán MoMo thành công")
                .data(paymentResponse)
                .build());
    }

    /**
     * Create MoMo payment for a booking by booking code
     * This is the recommended endpoint for frontend to use with booking code from
     * UI
     */
    @PostMapping("/momo/create-by-code/{bookingCode}")
    @Operation(summary = "Create MoMo payment by booking code", description = "Create a MoMo payment session using booking code (e.g., BK20260119021). "
            +
            "Returns payment URL to redirect user to MoMo payment page. " +
            "This endpoint is designed for users who want to continue payment from pending booking page.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid booking status or MoMo error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<ApiResponse<PaymentResponse>> createMomoPaymentByCode(
            @Parameter(description = "Booking Code (e.g., BK20260119021)", required = true) @PathVariable String bookingCode) {

        log.info("Creating MoMo payment for booking code: {}", bookingCode);

        // Find booking by code
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new com.example.Fuba_BE.exception.NotFoundException(
                        "Không tìm thấy booking với mã: " + bookingCode));

        PaymentResponse paymentResponse = momoPaymentService.createPayment(booking.getBookingId());

        return ResponseEntity.ok(ApiResponse.<PaymentResponse>builder()
                .success(true)
                .message("Tạo thanh toán MoMo thành công")
                .data(paymentResponse)
                .build());
    }

    /**
     * MoMo IPN callback endpoint
     * This is called by MoMo server after payment is processed
     */
    @PostMapping("/momo/ipn")
    @Operation(summary = "MoMo IPN callback", description = "Instant Payment Notification callback from MoMo. " +
            "Called by MoMo server to notify payment result. " +
            "Updates booking and seat status if payment is successful.")
    public ResponseEntity<Map<String, Object>> handleMomoIpn(@RequestBody MomoIpnRequest ipnRequest) {
        log.info("========================================");
        log.info("📥 Received MoMo IPN Callback");
        log.info("Order ID: {}", ipnRequest.getOrderId());
        log.info("Request ID: {}", ipnRequest.getRequestId());
        log.info("Result Code: {}", ipnRequest.getResultCode());
        log.info("Message: {}", ipnRequest.getMessage());
        log.info("Trans ID: {}", ipnRequest.getTransId());
        log.info("Amount: {}", ipnRequest.getAmount());
        log.info("Signature: {}", ipnRequest.getSignature());
        log.info("========================================");

        Map<String, Object> response = new HashMap<>();

        try {
            boolean success = momoPaymentService.handleIpnCallback(ipnRequest);

            if (success) {
                log.info("✅ IPN processed successfully for order: {}", ipnRequest.getOrderId());
                response.put("resultCode", 0);
                response.put("message", "Success");
            } else {
                log.warn("⚠️ IPN processing failed for order: {}", ipnRequest.getOrderId());
                response.put("resultCode", 1);
                response.put("message", "Failed to process payment");
            }
        } catch (Exception e) {
            log.error("❌ Error processing MoMo IPN for order {}: {}",
                    ipnRequest.getOrderId(), e.getMessage(), e);
            response.put("resultCode", 99);
            response.put("message", "Internal error: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * MoMo redirect endpoint after payment
     * User is redirected here after completing payment on MoMo
     */
    @GetMapping("/momo/redirect")
    @Operation(summary = "MoMo redirect callback", description = "Redirect URL after user completes payment on MoMo. " +
            "Returns payment status information.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleMomoRedirect(
            @RequestParam String orderId,
            @RequestParam(required = false) String requestId,
            @RequestParam Integer resultCode,
            @RequestParam(required = false) String message,
            @RequestParam(required = false) Long transId) {

        log.info("🔄 MoMo redirect: orderId={}, resultCode={}, transId={}", orderId, resultCode, transId);

        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("requestId", requestId);
        data.put("resultCode", resultCode);
        data.put("transId", transId);
        data.put("message", message);

        // Check booking status and sync if needed
        String bookingStatus = "Unknown";
        try {
            Booking booking = bookingRepository.findByBookingCode(orderId).orElse(null);
            if (booking != null) {
                bookingStatus = booking.getBookingStatus();
                data.put("bookingStatus", bookingStatus);

                // If payment successful but booking still Pending, IPN might not have arrived
                // yet
                if (resultCode == 0 && "Pending".equals(bookingStatus)) {
                    log.warn("⚠️ Payment successful but booking {} still Pending. IPN may be delayed.", orderId);

                    // Query MoMo status and manually complete payment if confirmed
                    if (requestId != null && !requestId.isEmpty()) {
                        try {
                            log.info("🔍 Querying MoMo status for orderId={}, requestId={}", orderId, requestId);
                            MomoPaymentResponse momoStatus = momoPaymentService.queryPaymentStatus(orderId, requestId);
                            data.put("momoStatus", momoStatus);

                            if (momoStatus.getResultCode() == 0) {
                                log.info("✅ MoMo confirms payment successful. Manually completing payment...");

                                // Build IPN request from redirect params to manually trigger payment completion
                                MomoIpnRequest manualIpn = new MomoIpnRequest();
                                manualIpn.setOrderId(orderId);
                                manualIpn.setRequestId(requestId);
                                manualIpn.setResultCode(0);
                                manualIpn.setMessage("Success");
                                manualIpn.setTransId(transId != null ? transId : 0L);
                                manualIpn.setAmount(momoStatus.getAmount());
                                manualIpn.setSkipSignatureCheck(true); // Skip signature check for manual IPN
                                // manualIpn.setOrderInfo(momoStatus.getOrderInfo() != null ?
                                // momoStatus.getOrderInfo() : "");
                                manualIpn.setPartnerCode(
                                        momoStatus.getPartnerCode() != null ? momoStatus.getPartnerCode() : "");
                                manualIpn.setResponseTime(System.currentTimeMillis());

                                boolean completed = momoPaymentService.handleIpnCallback(manualIpn);

                                if (completed) {
                                    log.info("🎉 Payment manually completed for booking {}", orderId);
                                    bookingStatus = "Paid";
                                    data.put("bookingStatus", bookingStatus);
                                    data.put("note", "Payment completed successfully.");
                                } else {
                                    log.error("❌ Failed to manually complete payment for booking {}", orderId);
                                    data.put("note",
                                            "Payment confirmed but processing failed. Please contact support.");
                                }
                            }
                        } catch (Exception e) {
                            log.error("❌ Error processing manual payment completion: {}", e.getMessage(), e);
                            data.put("note", "Payment confirmed. Processing in progress.");
                        }
                    } else {
                        data.put("note", "Payment confirmed. Booking update in progress.");
                    }
                } else if (resultCode == 0 && "Paid".equals(bookingStatus)) {
                    log.info("✅ Booking {} already marked as Paid", orderId);
                    data.put("note", "Payment completed successfully.");
                }
            } else {
                log.error("❌ Booking not found: {}", orderId);
                data.put("bookingStatus", "NotFound");
            }
        } catch (Exception e) {
            log.error("❌ Error checking booking status: {}", e.getMessage());
            data.put("bookingStatus", "Error");
        }

        boolean success = (resultCode == 0);
        String responseMessage = success
                ? "Thanh toán thành công"
                : "Thanh toán thất bại: " + message;

        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .success(success)
                .message(responseMessage)
                .data(data)
                .build());
    }

    /**
     * Query payment status from MoMo
     */
    @GetMapping("/momo/status")
    @Operation(summary = "Query MoMo payment status", description = "Query the current status of a MoMo payment")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment status retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Error querying payment status")
    })
    public ResponseEntity<ApiResponse<MomoPaymentResponse>> queryPaymentStatus(
            @Parameter(description = "Order ID (Booking code)", required = true) @RequestParam String orderId,
            @Parameter(description = "Request ID from payment creation", required = true) @RequestParam String requestId) {

        log.info("Querying MoMo payment status: orderId={}, requestId={}", orderId, requestId);

        MomoPaymentResponse status = momoPaymentService.queryPaymentStatus(orderId, requestId);

        // 🔥 AUTO-COMPLETE PAYMENT if MoMo confirms success but booking still Pending
        if (status.getResultCode() == 0) {
            try {
                Booking booking = bookingRepository.findByBookingCode(orderId).orElse(null);
                if (booking != null && "Pending".equals(booking.getBookingStatus())) {
                    log.warn("⚠️ MoMo confirms payment successful but booking {} still Pending. Auto-completing...",
                            orderId);

                    // Manually trigger payment completion
                    MomoIpnRequest manualIpn = new MomoIpnRequest();
                    manualIpn.setOrderId(orderId);
                    manualIpn.setRequestId(requestId);
                    manualIpn.setResultCode(0);
                    manualIpn.setMessage("Success");
                    manualIpn.setTransId(0L); // Query response doesn't have transId
                    manualIpn.setAmount(status.getAmount());
                    manualIpn.setSkipSignatureCheck(true); // Skip signature - already verified via queryPaymentStatus

                    boolean completed = momoPaymentService.handleIpnCallback(manualIpn);

                    if (completed) {
                        log.info("🎉 Payment auto-completed for booking {}", orderId);
                    } else {
                        log.error("❌ Failed to auto-complete payment for booking {}", orderId);
                    }
                }
            } catch (Exception e) {
                log.error("❌ Error auto-completing payment: {}", e.getMessage(), e);
            }
        }

        return ResponseEntity.ok(ApiResponse.<MomoPaymentResponse>builder()
                .success(status.getResultCode() == 0)
                .message(status.getMessage())
                .data(status)
                .build());
    }

    /**
     * Get booking payment status by order ID
     * Frontend can call this to check if payment has been processed
     */
    @GetMapping("/booking-status/{orderId}")
    @Operation(summary = "Get booking payment status", description = "Check the current payment status of a booking by order ID (booking code)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBookingPaymentStatus(
            @Parameter(description = "Order ID (Booking code)", required = true) @PathVariable String orderId) {

        log.info("📊 Checking payment status for orderId: {}", orderId);

        Map<String, Object> data = new HashMap<>();

        try {
            Booking booking = bookingRepository.findByBookingCode(orderId)
                    .orElseThrow(() -> new RuntimeException("Booking not found: " + orderId));

            data.put("orderId", orderId);
            data.put("bookingStatus", booking.getBookingStatus());
            data.put("totalAmount", booking.getTotalAmount());
            data.put("isPaid", "Paid".equals(booking.getBookingStatus()));

            String statusMessage;
            switch (booking.getBookingStatus()) {
                case "Paid":
                    statusMessage = "Thanh toán thành công. Vé đã được xác nhận.";
                    break;
                case "Pending":
                    statusMessage = "Đang chờ xử lý thanh toán...";
                    break;
                case "PaymentFailed":
                    statusMessage = "Thanh toán thất bại.";
                    break;
                case "Held":
                    statusMessage = "Đang giữ chỗ. Chưa thanh toán.";
                    break;
                case "Expired":
                    statusMessage = "Đơn hàng đã hết hạn.";
                    break;
                default:
                    statusMessage = "Trạng thái: " + booking.getBookingStatus();
            }

            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .message(statusMessage)
                    .data(data)
                    .build());

        } catch (Exception e) {
            log.error("❌ Error getting booking status: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Không tìm thấy đơn hàng")
                    .data(data)
                    .build());
        }
    }
}
