package com.example.Fuba_BE.service.Booking;

import java.util.List;

import com.example.Fuba_BE.dto.Booking.BookingConfirmRequest;
import com.example.Fuba_BE.dto.Booking.BookingPreviewResponse;
import com.example.Fuba_BE.dto.Booking.BookingResponse;

/**
 * Service interface for booking operations.
 * Handles ticket booking with seat lock validation.
 */
public interface IBookingService {

    /**
     * Preview/validate booking before confirmation.
     * Checks if all seats are locked by the user and returns booking summary.
     *
     * @param tripId  The trip ID
     * @param seatIds List of seat IDs to book
     * @param userId  User ID who locked the seats
     * @return BookingPreviewResponse with validation result and summary
     */
    BookingPreviewResponse previewBooking(Integer tripId, List<Integer> seatIds, String userId);

    /**
     * Confirm booking after seats have been locked.
     * Validates seat lock ownership and creates booking + tickets.
     *
     * @param request BookingConfirmRequest with booking details
     * @return BookingResponse with confirmed booking details
     */
    BookingResponse confirmBooking(BookingConfirmRequest request);

    /**
     * Get booking by ID
     *
     * @param bookingId The booking ID
     * @return BookingResponse with booking details
     */
    BookingResponse getBookingById(Integer bookingId);

    /**
     * Get booking by booking code
     *
     * @param bookingCode The unique booking code
     * @return BookingResponse with booking details
     */
    BookingResponse getBookingByCode(String bookingCode);

    /**
     * Get all bookings for a customer
     *
     * @param customerId The customer ID
     * @return List of BookingResponse
     */
    List<BookingResponse> getBookingsByCustomerId(Integer customerId);

    /**
     * Get all bookings for a trip
     *
     * @param tripId The trip ID
     * @return List of BookingResponse
     */
    List<BookingResponse> getBookingsByTripId(Integer tripId);

    /**
     * Cancel a booking
     *
     * @param bookingId The booking ID
     * @param userId    User requesting cancellation
     * @return BookingResponse with updated status
     */
    BookingResponse cancelBooking(Integer bookingId, String userId);

    /**
     * Get bookings by phone number (for guest lookup)
     *
     * @param phone The customer phone number
     * @return List of BookingResponse
     */
    List<BookingResponse> getBookingsByPhone(String phone);
}
