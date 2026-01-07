package com.example.Fuba_BE.controller;

import com.example.Fuba_BE.dto.Booking.BookingConfirmRequest;
import com.example.Fuba_BE.dto.Booking.BookingPreviewResponse;
import com.example.Fuba_BE.dto.Booking.BookingResponse;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.service.Booking.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for booking operations.
 * Handles ticket booking with seat lock validation.
 */
@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Tag(name = "Booking", description = "APIs for managing ticket bookings")
@Slf4j
public class BookingController {

    private final BookingService bookingService;

    /**
     * Preview/validate booking before confirmation.
     * Checks if all seats are locked by the user.
     */
    @GetMapping("/preview")
    @Operation(
            summary = "Preview booking",
            description = "Validate seats and preview booking details before confirmation. " +
                    "All seats must be locked by the same user."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Preview generated successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters"
            )
    })
    public ResponseEntity<ApiResponse<BookingPreviewResponse>> previewBooking(
            @Parameter(description = "Trip ID", required = true)
            @RequestParam Integer tripId,
            @Parameter(description = "List of seat IDs", required = true)
            @RequestParam List<Integer> seatIds,
            @Parameter(description = "User ID who locked the seats", required = true)
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
    @Operation(
            summary = "Confirm booking",
            description = "Confirm booking after seats have been locked. " +
                    "Validates seat lock ownership and creates booking with tickets. " +
                    "All seats must be locked by the same user and locks must not be expired."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Booking confirmed successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or seat lock validation failed"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Trip or seat not found"
            )
    })
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
    @Operation(
            summary = "Get booking by ID",
            description = "Retrieve booking details by booking ID"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Booking found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Booking not found"
            )
    })
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(
            @Parameter(description = "Booking ID", required = true)
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
    @Operation(
            summary = "Get booking by code",
            description = "Retrieve booking details by unique booking code"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Booking found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Booking not found"
            )
    })
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingByCode(
            @Parameter(description = "Booking code", required = true)
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
    @Operation(
            summary = "Get bookings by customer",
            description = "Retrieve all bookings for a specific customer"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Bookings retrieved successfully"
            )
    })
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookingsByCustomerId(
            @Parameter(description = "Customer ID", required = true)
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
    @Operation(
            summary = "Get bookings by trip",
            description = "Retrieve all bookings for a specific trip"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Bookings retrieved successfully"
            )
    })
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookingsByTripId(
            @Parameter(description = "Trip ID", required = true)
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
    @Operation(
            summary = "Cancel booking",
            description = "Cancel a booking and release all associated seats"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Booking cancelled successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Booking cannot be cancelled"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Booking not found"
            )
    })
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @Parameter(description = "Booking ID", required = true)
            @PathVariable Integer bookingId,
            @Parameter(description = "User ID requesting cancellation", required = true)
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
    @Operation(
            summary = "Get bookings by phone",
            description = "Retrieve all bookings for a specific phone number (useful for guest booking lookup)"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Bookings retrieved successfully"
            )
    })
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookingsByPhone(
            @Parameter(description = "Customer phone number", required = true)
            @PathVariable String phone) {
        
        List<BookingResponse> bookings = bookingService.getBookingsByPhone(phone);
        
        return ResponseEntity.ok(ApiResponse.<List<BookingResponse>>builder()
                .success(true)
                .message("Lấy danh sách booking thành công")
                .data(bookings)
                .build());
    }
}
