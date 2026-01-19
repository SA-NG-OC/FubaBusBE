package com.example.Fuba_BE.controller;

import com.example.Fuba_BE.dto.Booking.*;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.security.UserPrincipal;
import com.example.Fuba_BE.service.Booking.IBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    /* ================= LIST ALL WITH FILTERS ================= */

    @GetMapping
    @Operation(summary = "Get all bookings with pagination and filtering", 
               description = "Retrieve all bookings with optional status filter and search by booking code, customer name, or phone")
    public ResponseEntity<ApiResponse<BookingPageResponse>> getAllBookings(
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String sortDirection) {

        log.info("Get all bookings: page={}, size={}, status={}, search={}", page, size, status, search);

        BookingFilterRequest filterRequest = BookingFilterRequest.builder()
                .page(page)
                .size(size)
                .status(status)
                .search(search)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        BookingPageResponse response = bookingService.getAllBookings(filterRequest);

        return ResponseEntity.ok(ApiResponse.<BookingPageResponse>builder()
                .success(true)
                .message("Lấy danh sách booking thành công")
                .data(response)
                .build());
    }

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

        // Extract actual booking code if it contains timestamp (format: BK20260119023-1768805286103)
        String actualBookingCode = bookingCode.contains("-") 
            ? bookingCode.substring(0, bookingCode.lastIndexOf("-")) 
            : bookingCode;
        
        log.debug("Looking for booking with code: {} (from path param: {})", actualBookingCode, bookingCode);

        return ResponseEntity.ok(ApiResponse.<BookingResponse>builder()
                .success(true)
                .message("Lấy thông tin booking thành công")
                .data(bookingService.getBookingByCode(actualBookingCode))
                .build());
    }

    @GetMapping("/ticket/{ticketCode}")
    @Operation(summary = "Get booking by ticket code", 
               description = "Retrieve booking information using ticket code (e.g., TK20260106002)")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingByTicketCode(
            @PathVariable String ticketCode) {

        return ResponseEntity.ok(ApiResponse.<BookingResponse>builder()
                .success(true)
                .message("Lấy thông tin booking thành công")
                .data(bookingService.getBookingByTicketCode(ticketCode))
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

    @GetMapping("/email/{email}")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookingsByEmail(
            @PathVariable String email) {

        return ResponseEntity.ok(ApiResponse.<List<BookingResponse>>builder()
                .success(true)
                .message("Lấy danh sách booking thành công")
                .data(bookingService.getBookingsByEmail(email))
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

    @PostMapping("/{bookingId}/confirm")
    @Operation(summary = "Confirm a booking", 
               description = "Change booking status from Pending/Held to Paid and update related tickets and seats")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmBooking(
            @PathVariable Integer bookingId) {

        log.info("Confirm booking with ID: {}", bookingId);

        BookingResponse response = bookingService.confirmBookingById(bookingId);

        return ResponseEntity.ok(ApiResponse.<BookingResponse>builder()
                .success(true)
                .message("Xác nhận booking thành công")
                .data(response)
                .build());
    }

    @PostMapping("/reschedule")
    @Operation(summary = "Reschedule booking to a new trip",
            description = "Cancel old booking and create new booking on a different trip. " +
                    "Handles refund if new trip is cheaper, or extra fee if more expensive. " +
                    "Must be done at least 12 hours before old trip departure.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reschedule successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or policy violation"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Booking or trip not found")
    })
    public ResponseEntity<ApiResponse<RescheduleResponse>> rescheduleBooking(
            @Valid @RequestBody RescheduleRequest request) {

        log.info("Reschedule request: bookingId={}, newTripId={}", 
                request.getOldBookingId(), request.getNewTripId());

        RescheduleResponse response = bookingService.rescheduleBooking(request);

        return ResponseEntity.ok(ApiResponse.<RescheduleResponse>builder()
                .success(true)
                .message(response.getMessage())
                .data(response)
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

    /* ================= MY TICKETS ================= */

    @GetMapping("/my-tickets")
    @Operation(summary = "Get my tickets with pagination", 
               description = "Get bookings of current authenticated user. Filter by status: Upcoming (Held + Paid with future departure), Completed, Cancelled")
    public ResponseEntity<ApiResponse<BookingPageResponse>> getMyTickets(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {

        log.info("Get my tickets for user {}: status={}, page={}, size={}", 
                currentUser.getUserId(), status, page, size);

        BookingPageResponse response = bookingService.getMyTickets(
                currentUser.getUserId(), status, page, size);

        return ResponseEntity.ok(ApiResponse.<BookingPageResponse>builder()
                .success(true)
                .message("Lấy danh sách vé của tôi thành công")
                .data(response)
                .build());
    }

    @GetMapping("/my-tickets/count")
    @Operation(summary = "Count my tickets by status", 
               description = "Count bookings of current authenticated user grouped by status: Upcoming, Completed, Cancelled")
    public ResponseEntity<ApiResponse<TicketCountResponse>> getMyTicketsCount(
            @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("Get my tickets count for user {}", currentUser.getUserId());

        TicketCountResponse response = bookingService.getMyTicketsCount(currentUser.getUserId());

        return ResponseEntity.ok(ApiResponse.<TicketCountResponse>builder()
                .success(true)
                .message("Lấy thống kê vé của tôi thành công")
                .data(response)
                .build());
    }
}
