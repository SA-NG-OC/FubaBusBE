package com.example.Fuba_BE.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.Fuba_BE.dto.Booking.BookingConfirmRequest;
import com.example.Fuba_BE.dto.Booking.BookingPreviewResponse;
import com.example.Fuba_BE.dto.Booking.BookingResponse;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.service.Booking.IBookingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for booking operations.
 * Handles ticket booking with seat lock validation.
 */
@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Slf4j
public class BookingController {

    private final IBookingService bookingService;

    /**
     * Preview/validate booking before confirmation.
     * Checks if all seats are locked by the user.
     */
    @GetMapping("/preview")
    public ResponseEntity<ApiResponse<BookingPreviewResponse>> previewBooking(
                
            @RequestParam Integer tripId,

            @RequestParam List<Integer> seatIds,
           
            @RequestParam String userId) {
        
        log.info("Preview booking request: tripId={}, seatIds={}, userId={}", tripId, seatIds, userId);
        
        BookingPreviewResponse preview = bookingService.previewBooking(tripId, seatIds, userId);
        
        return ResponseEntity.ok(ApiResponse.<BookingPreviewResponse>builder()
                .success(preview.isValid())
                .message(preview.getMessage())
                .data(preview)
                .build());
    }

    /**
     * Confirm booking after seats have been locked.
     */
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmBooking(
            @Valid @RequestBody BookingConfirmRequest request) {
        
        log.info("Confirm booking request: tripId={}, seatIds={}, userId={}", 
                request.getTripId(), request.getSeatIds(), request.getUserId());
        
        BookingResponse booking = bookingService.confirmBooking(request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<BookingResponse>builder()
                        .success(true)
                        .message("Đặt vé thành công")
                        .data(booking)
                        .build());
    }

    /**
     * Get booking by ID
     */
    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(
            @PathVariable Integer bookingId) {
        
        BookingResponse booking = bookingService.getBookingById(bookingId);
        
        return ResponseEntity.ok(ApiResponse.<BookingResponse>builder()
                .success(true)
                .message("Lấy thông tin booking thành công")
                .data(booking)
                .build());
    }

    /**
     * Get booking by booking code
     */
    @GetMapping("/code/{bookingCode}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingByCode(
            @PathVariable String bookingCode) {
        
        BookingResponse booking = bookingService.getBookingByCode(bookingCode);
        
        return ResponseEntity.ok(ApiResponse.<BookingResponse>builder()
                .success(true)
                .message("Lấy thông tin booking thành công")
                .data(booking)
                .build());
    }

    /**
     * Get all bookings for a customer
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookingsByCustomerId(
            @PathVariable Integer customerId) {
        
        List<BookingResponse> bookings = bookingService.getBookingsByCustomerId(customerId);
        
        return ResponseEntity.ok(ApiResponse.<List<BookingResponse>>builder()
                .success(true)
                .message("Lấy danh sách booking thành công")
                .data(bookings)
                .build());
    }

    /**
     * Get all bookings for a trip
     */
    @GetMapping("/trip/{tripId}")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookingsByTripId(
            @PathVariable Integer tripId) {
        
        List<BookingResponse> bookings = bookingService.getBookingsByTripId(tripId);
        
        return ResponseEntity.ok(ApiResponse.<List<BookingResponse>>builder()
                .success(true)
                .message("Lấy danh sách booking thành công")
                .data(bookings)
                .build());
    }

    /**
     * Cancel a booking
     */
    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
         
            @PathVariable Integer bookingId,
            
            @RequestParam String userId) {
        
        log.info("Cancel booking request: bookingId={}, userId={}", bookingId, userId);
        
        BookingResponse booking = bookingService.cancelBooking(bookingId, userId);
        
        return ResponseEntity.ok(ApiResponse.<BookingResponse>builder()
                .success(true)
                .message("Hủy booking thành công")
                .data(booking)
                .build());
    }

    /**
     * Get bookings by phone number (for guest lookup)
     */
    @GetMapping("/phone/{phone}")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookingsByPhone(
            @PathVariable String phone) {
        
        List<BookingResponse> bookings = bookingService.getBookingsByPhone(phone);
        
        return ResponseEntity.ok(ApiResponse.<List<BookingResponse>>builder()
                .success(true)
                .message("Lấy danh sách booking thành công")
                .data(bookings)
                .build());
    }
}
