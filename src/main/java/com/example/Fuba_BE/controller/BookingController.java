package com.example.Fuba_BE.controller;

import com.example.Fuba_BE.dto.Booking.*;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.service.Booking.IBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for booking operations.
 */
@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Slf4j
public class BookingController {

    private final IBookingService bookingService;

    /* ================= PREVIEW ================= */

    @GetMapping("/preview")
    public ResponseEntity<ApiResponse<BookingPreviewResponse>> previewBooking(
            @RequestParam Integer tripId,
            @RequestParam List<Integer> seatIds,
            @RequestParam String userId) {

        log.info("Preview booking: tripId={}, seatIds={}, userId={}", tripId, seatIds, userId);

        BookingPreviewResponse preview =
                bookingService.previewBooking(tripId, seatIds, userId);

        return ResponseEntity.ok(ApiResponse.<BookingPreviewResponse>builder()
                .success(preview.isValid())
                .message(preview.getMessage())
                .data(preview)
                .build());
    }

    /* ================= CONFIRM ================= */

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmBooking(
            @Valid @RequestBody BookingConfirmRequest request) {

        BookingResponse booking = bookingService.confirmBooking(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<BookingResponse>builder()
                        .success(true)
                        .message("Đặt vé thành công")
                        .data(booking)
                        .build());
    }

    /* ================= COUNTER ================= */

    @PostMapping("/counter")
    @Operation(summary = "Create counter booking")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404")
    })
    public ResponseEntity<ApiResponse<BookingResponse>> createCounterBooking(
            @Valid @RequestBody CounterBookingRequest request) {

        BookingResponse booking =
                bookingService.createCounterBooking(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<BookingResponse>builder()
                        .success(true)
                        .message("Bán vé tại quầy thành công")
                        .data(booking)
                        .build());
    }

    /* ================= QUERY ================= */

    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(
            @PathVariable Integer bookingId) {

        return ResponseEntity.ok(ApiResponse.<BookingResponse>builder()
                .success(true)
                .message("Lấy thông tin booking thành công")
                .data(bookingService.getBookingById(bookingId))
                .build());
    }

    @GetMapping("/code/{bookingCode}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingByCode(
            @PathVariable String bookingCode) {

        return ResponseEntity.ok(ApiResponse.<BookingResponse>builder()
                .success(true)
                .message("Lấy thông tin booking thành công")
                .data(bookingService.getBookingByCode(bookingCode))
                .build());
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookingsByCustomerId(
            @Parameter(description = "Customer ID")
            @PathVariable Integer customerId,
            @RequestParam(required = false) String status) {

        List<BookingResponse> bookings =
                bookingService.getBookingsByCustomerId(customerId, status);

        return ResponseEntity.ok(ApiResponse.<List<BookingResponse>>builder()
                .success(true)
                .message("Lấy danh sách booking thành công")
                .data(bookings)
                .build());
    }

    @GetMapping("/trip/{tripId}")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookingsByTripId(
            @PathVariable Integer tripId) {

        return ResponseEntity.ok(ApiResponse.<List<BookingResponse>>builder()
                .success(true)
                .message("Lấy danh sách booking thành công")
                .data(bookingService.getBookingsByTripId(tripId))
                .build());
    }

    @GetMapping("/phone/{phone}")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookingsByPhone(
            @PathVariable String phone) {

        return ResponseEntity.ok(ApiResponse.<List<BookingResponse>>builder()
                .success(true)
                .message("Lấy danh sách booking thành công")
                .data(bookingService.getBookingsByPhone(phone))
                .build());
    }

    /* ================= ACTION ================= */

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable Integer bookingId,
            @RequestParam String userId) {

        return ResponseEntity.ok(ApiResponse.<BookingResponse>builder()
                .success(true)
                .message("Hủy booking thành công")
                .data(bookingService.cancelBooking(bookingId, userId))
                .build());
    }

    @PostMapping("/{bookingId}/payment")
    public ResponseEntity<ApiResponse<BookingResponse>> processPayment(
            @PathVariable Integer bookingId,
            @RequestBody(required = false) Map<String, Object> paymentDetails) {

        return ResponseEntity.ok(ApiResponse.<BookingResponse>builder()
                .success(true)
                .message("Thanh toán thành công")
                .data(bookingService.processPayment(bookingId, paymentDetails))
                .build());
    }
}
