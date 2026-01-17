package com.example.Fuba_BE.service.payment;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.example.Fuba_BE.config.MomoConfig;
import com.example.Fuba_BE.domain.entity.Booking;
import com.example.Fuba_BE.domain.entity.Ticket;
import com.example.Fuba_BE.domain.entity.TripSeat;
import com.example.Fuba_BE.domain.enums.TicketStatus;
import com.example.Fuba_BE.dto.payment.MomoIpnRequest;
import com.example.Fuba_BE.dto.payment.MomoPaymentRequest;
import com.example.Fuba_BE.dto.payment.MomoPaymentResponse;
import com.example.Fuba_BE.dto.payment.PaymentResponse;
import com.example.Fuba_BE.exception.BadRequestException;
import com.example.Fuba_BE.exception.NotFoundException;
import com.example.Fuba_BE.mapper.BookingMapper;
import com.example.Fuba_BE.repository.BookingRepository;
import com.example.Fuba_BE.repository.TicketRepository;
import com.example.Fuba_BE.repository.TripSeatRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of MoMo payment service
 * Handles payment creation, IPN callback, and signature verification
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MomoPaymentServiceImpl implements MomoPaymentService {

    private final MomoConfig momoConfig;
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final TripSeatRepository tripSeatRepository;
    private final BookingMapper bookingMapper;
    private final RestTemplate restTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String HMAC_SHA256 = "HmacSHA256";

    @Override
    @Transactional
    public PaymentResponse createPayment(Integer bookingId) {
        log.info("Creating MoMo payment for booking ID: {}", bookingId);

        // 1. Get booking and validate status
        Booking booking = bookingRepository.findByIdWithLock(bookingId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy booking với ID: " + bookingId));

        if (!"Held".equals(booking.getBookingStatus())) {
            throw new BadRequestException("Booking không ở trạng thái Held. Hiện tại: " + booking.getBookingStatus());
        }

        // 2. Generate unique request ID and prepare payment data
        String requestId = UUID.randomUUID().toString();
        String orderId = booking.getBookingCode();
        String amount = booking.getTotalAmount().toBigInteger().toString();
        String orderInfo = "Thanh toan ve xe - " + orderId;
        String extraData = ""; // Can encode additional data in base64 if needed
        String lang = "vi";

        // 3. Build signature string (alphabetical order as per MoMo docs)
        String rawSignature = String.format(
                "accessKey=%s&amount=%s&extraData=%s&ipnUrl=%s&orderId=%s&orderInfo=%s&partnerCode=%s&redirectUrl=%s&requestId=%s&requestType=%s",
                momoConfig.getAccessKey(),
                amount,
                extraData,
                momoConfig.getIpnUrl(),
                orderId,
                orderInfo,
                momoConfig.getPartnerCode(),
                momoConfig.getRedirectUrl(),
                requestId,
                momoConfig.getRequestType());

        log.debug("Raw signature string: {}", rawSignature);

        // 4. Compute HMAC-SHA256 signature
        String signature = computeHmacSha256(rawSignature, momoConfig.getSecretKey());

        // 5. Build request payload
        MomoPaymentRequest paymentRequest = MomoPaymentRequest.builder()
                .partnerCode(momoConfig.getPartnerCode())
                .accessKey(momoConfig.getAccessKey())
                .requestId(requestId)
                .amount(amount)
                .orderId(orderId)
                .orderInfo(orderInfo)
                .redirectUrl(momoConfig.getRedirectUrl())
                .ipnUrl(momoConfig.getIpnUrl())
                .requestType(momoConfig.getRequestType())
                .extraData(extraData)
                .lang(lang)
                .signature(signature)
                .build();

        log.info("Sending payment request to MoMo: orderId={}, amount={}", orderId, amount);

        // 6. Send request to MoMo API
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<MomoPaymentRequest> requestEntity = new HttpEntity<>(paymentRequest, headers);

            ResponseEntity<MomoPaymentResponse> responseEntity = restTemplate.exchange(
                    momoConfig.getEndpoint(),
                    HttpMethod.POST,
                    requestEntity,
                    MomoPaymentResponse.class);

            MomoPaymentResponse momoResponse = responseEntity.getBody();

            if (momoResponse == null) {
                throw new BadRequestException("Không nhận được phản hồi từ MoMo");
            }

            log.info("MoMo response: resultCode={}, message={}", momoResponse.getResultCode(),
                    momoResponse.getMessage());

            // 7. Check result code
            if (momoResponse.getResultCode() != 0) {
                throw new BadRequestException("MoMo payment error: " + momoResponse.getMessage());
            }

            // 8. Update booking status to "Pending" (waiting for payment)
            // This works for both Held -> Pending and Expired -> Pending (reactivation)
            booking.setBookingStatus("Pending");
            booking.setHoldExpiry(LocalDateTime.now().plusMinutes(15)); // Extend expiry to 15 minutes from now
            bookingRepository.save(booking);

            log.info("Booking {} status updated to Pending, expiry extended to: {}",
                    booking.getBookingCode(), booking.getHoldExpiry());

            // 9. Build and return response
            List<Ticket> tickets = ticketRepository.findByBookingId(bookingId);

            return PaymentResponse.builder()
                    .payUrl(momoResponse.getPayUrl())
                    .deeplink(momoResponse.getDeeplink())
                    .qrCodeUrl(momoResponse.getQrCodeUrl())
                    .orderId(orderId)
                    .requestId(requestId)
                    .booking(bookingMapper.toBookingResponse(booking, booking.getTrip(), tickets))
                    .message("Tạo thanh toán thành công. Vui lòng thanh toán qua MoMo.")
                    .build();

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating MoMo payment: {}", e.getMessage(), e);
            throw new BadRequestException("Lỗi khi tạo thanh toán MoMo: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean handleIpnCallback(MomoIpnRequest ipnRequest) {
        log.info("Received MoMo IPN callback: orderId={}, resultCode={}",
                ipnRequest.getOrderId(), ipnRequest.getResultCode());

        // 1. Verify signature
        // Build rawSignature in ALPHABETICAL order of field names (MoMo requirement)
        String rawSignature = String.format(
                "accessKey=%s&amount=%s&extraData=%s&message=%s&orderId=%s&orderInfo=%s&orderType=%s&partnerCode=%s&payType=%s&requestId=%s&responseTime=%s&resultCode=%s&transId=%s",
                momoConfig.getAccessKey(),
                ipnRequest.getAmount(),
                ipnRequest.getExtraData() != null ? ipnRequest.getExtraData() : "",
                ipnRequest.getMessage(),
                ipnRequest.getOrderId(),
                ipnRequest.getOrderInfo(),
                ipnRequest.getOrderType() != null ? ipnRequest.getOrderType() : "",
                ipnRequest.getPartnerCode(),
                ipnRequest.getPayType() != null ? ipnRequest.getPayType() : "",
                ipnRequest.getRequestId(),
                ipnRequest.getResponseTime(),
                ipnRequest.getResultCode(),
                ipnRequest.getTransId());

        log.debug("IPN rawSignature: {}", rawSignature);
        log.debug("IPN signature: {}", ipnRequest.getSignature());

        if (!verifySignature(rawSignature, ipnRequest.getSignature())) {
            log.error("❌ Invalid IPN signature for orderId: {}", ipnRequest.getOrderId());
            return false;
        }

        // 2. Find booking by order ID (booking code) WITH PESSIMISTIC LOCK to prevent
        // race conditions
        log.debug("Looking for booking with code: {}", ipnRequest.getOrderId());
        Booking booking = bookingRepository.findByBookingCodeWithLock(ipnRequest.getOrderId())
                .orElse(null);

        if (booking == null) {
            log.error("❌ Booking not found for orderId: {}", ipnRequest.getOrderId());
            return false;
        }

        log.info("✅ Found booking: ID={}, Code={}, Status={}",
                booking.getBookingId(), booking.getBookingCode(), booking.getBookingStatus());

        // 2.5. Handle duplicate IPN callbacks (idempotency check)
        if ("Paid".equals(booking.getBookingStatus())) {
            log.warn("⚠️ Booking {} already Paid. Ignoring duplicate IPN callback.", booking.getBookingCode());
            return true; // Return success to acknowledge the IPN
        }

        // 2.6. Reject if booking already expired (should not resurrect expired
        // bookings)
        if ("Expired".equals(booking.getBookingStatus())) {
            log.error("❌ Booking {} is Expired. Cannot accept payment for expired booking.", booking.getBookingCode());
            return false;
        }

        // 3. Check result code (0 = success)
        if (ipnRequest.getResultCode() == 0) {
            log.info("💰 Payment successful for booking: {}", booking.getBookingCode());

            // 3.5. Verify all seats are still Held and available for this booking
            log.debug("Validating seats for booking {}...", booking.getBookingCode());
            List<Ticket> tickets = ticketRepository.findByBookingId(booking.getBookingId());
            log.debug("Found {} tickets for booking {}", tickets.size(), booking.getBookingCode());

            for (Ticket ticket : tickets) {
                TripSeat seat = ticket.getSeat();
                if (seat == null) {
                    log.error("❌ Seat not found for ticket {} in booking {}",
                            ticket.getTicketCode(), booking.getBookingCode());
                    return false;
                }

                log.debug("Checking seat {}: current status = {}", seat.getSeatNumber(), seat.getStatus());

                // Critical check: seat must be Held, not Available or Booked
                if (!"Held".equals(seat.getStatus())) {
                    log.error(
                            "❌ Seat {} status is {} (expected Held) for booking {}. Payment rejected to prevent double booking.",
                            seat.getSeatNumber(), seat.getStatus(), booking.getBookingCode());
                    return false;
                }

                log.debug("✅ Seat {} validation passed", seat.getSeatNumber());
            }

            log.info("All {} seats validated successfully for booking {}", tickets.size(), booking.getBookingCode());

            // 4. Update booking status to Paid
            booking.setBookingStatus("Paid");
            booking.setHoldExpiry(null);
            booking.setUpdatedAt(LocalDateTime.now());
            bookingRepository.save(booking);

            log.info("✅ Booking {} status updated to Paid", booking.getBookingCode());

            // 5. Update tickets and seats
            for (Ticket ticket : tickets) {
                // Update ticket status
                ticket.setTicketStatus(TicketStatus.CONFIRMED.getDisplayName());
                ticketRepository.save(ticket);

                log.debug("✅ Ticket {} status updated to Confirmed", ticket.getTicketCode());

                // Update seat status to Booked (already validated above)
                TripSeat seat = ticket.getSeat();
                seat.book();
                tripSeatRepository.save(seat);

                log.debug("✅ Seat {} status updated to Booked", seat.getSeatNumber());

                // Broadcast seat status change via WebSocket (non-critical, catch errors)
                try {
                    broadcastSeatUpdate(booking.getTrip().getTripId(), seat);
                    log.debug("📢 Broadcasted seat {} update via WebSocket", seat.getSeatNumber());
                } catch (Exception e) {
                    log.warn("⚠️ Failed to broadcast seat update via WebSocket (non-critical): {}", e.getMessage());
                    // Continue - don't fail the payment just because WebSocket failed
                }
            }

            log.info("🎉 Booking {} payment completed successfully. {} tickets confirmed, {} seats booked.",
                    booking.getBookingCode(), tickets.size(), tickets.size());

            return true;
        } else {
            // Payment failed
            log.warn("Payment failed for booking {}: {}", booking.getBookingCode(), ipnRequest.getMessage());

            // Optionally update booking status or leave as Pending for retry
            // booking.setBookingStatus("PaymentFailed");
            // bookingRepository.save(booking);

            return false;
        }
    }

    @Override
    public MomoPaymentResponse queryPaymentStatus(String orderId, String requestId) {
        log.info("Querying payment status: orderId={}, requestId={}", orderId, requestId);

        String rawSignature = String.format(
                "accessKey=%s&orderId=%s&partnerCode=%s&requestId=%s",
                momoConfig.getAccessKey(),
                orderId,
                momoConfig.getPartnerCode(),
                requestId);

        String signature = computeHmacSha256(rawSignature, momoConfig.getSecretKey());

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("partnerCode", momoConfig.getPartnerCode());
        requestBody.put("accessKey", momoConfig.getAccessKey());
        requestBody.put("requestId", requestId);
        requestBody.put("orderId", orderId);
        requestBody.put("signature", signature);
        requestBody.put("lang", "vi");

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

            // Use query endpoint
            String queryEndpoint = momoConfig.getEndpoint().replace("/create", "/query");

            ResponseEntity<MomoPaymentResponse> responseEntity = restTemplate.exchange(
                    queryEndpoint,
                    HttpMethod.POST,
                    requestEntity,
                    MomoPaymentResponse.class);

            return responseEntity.getBody();

        } catch (Exception e) {
            log.error("Error querying payment status: {}", e.getMessage(), e);
            throw new BadRequestException("Lỗi khi truy vấn trạng thái thanh toán: " + e.getMessage());
        }
    }

    @Override
    public boolean verifySignature(String rawData, String signature) {
        String computedSignature = computeHmacSha256(rawData, momoConfig.getSecretKey());
        return computedSignature.equals(signature);
    }

    /**
     * Compute HMAC-SHA256 signature
     */
    private String computeHmacSha256(String data, String secretKey) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (Exception e) {
            log.error("Error computing HMAC-SHA256: {}", e.getMessage(), e);
            throw new RuntimeException("Error computing signature", e);
        }
    }

    /**
     * Broadcast seat status update via WebSocket
     */
    private void broadcastSeatUpdate(Integer tripId, TripSeat seat) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("seatId", seat.getSeatId());
            message.put("seatNumber", seat.getSeatNumber());
            message.put("status", seat.getStatus());
            message.put("lockedBy", seat.getLockedBy());
            message.put("holdExpiry", seat.getHoldExpiry());
            message.put("timestamp", LocalDateTime.now().toString());

            String destination = "/topic/trips/" + tripId + "/seats";
            messagingTemplate.convertAndSend(destination, (Object) message);

            log.debug("Broadcast seat update for seat {} on trip {}", seat.getSeatId(), tripId);
        } catch (Exception e) {
            log.error("Failed to broadcast seat update: {}", e.getMessage());
        }
    }
}
