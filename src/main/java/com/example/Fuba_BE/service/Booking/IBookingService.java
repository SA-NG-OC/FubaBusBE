package com.example.Fuba_BE.service.Booking;

import java.util.List;
import java.util.Map;

import com.example.Fuba_BE.dto.Booking.BookingConfirmRequest;
import com.example.Fuba_BE.dto.Booking.BookingFilterRequest;
import com.example.Fuba_BE.dto.Booking.BookingPageResponse;
import com.example.Fuba_BE.dto.Booking.BookingPreviewResponse;
import com.example.Fuba_BE.dto.Booking.BookingResponse;
import com.example.Fuba_BE.dto.Booking.CounterBookingRequest;
import com.example.Fuba_BE.dto.Booking.RescheduleRequest;
import com.example.Fuba_BE.dto.Booking.RescheduleResponse;

/**
 * Service interface for booking operations.
 * Handles ticket booking, seat locking, payments, and cancellations.
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
     * Confirm booking after seats have been locked (Online booking).
     * Validates seat lock ownership and creates booking + tickets.
     *
     * @param request BookingConfirmRequest with booking details
     * @return BookingResponse with confirmed booking details
     */
    BookingResponse confirmBooking(BookingConfirmRequest request);

    /**
     * Create counter booking (direct booking without seat locking).
     * Creates booking with BookingType=Counter and BookingStatus=Paid.
     *
     * @param request CounterBookingRequest with booking details
     * @return BookingResponse with created booking details
     */
    BookingResponse createCounterBooking(CounterBookingRequest request);

    /**
     * Process payment for a booking.
     * Changes booking status from Held to Paid and seats from Held to Booked.
     *
     * @param bookingId      The booking ID to process payment for
     * @param paymentDetails Payment details (Map for flexibility with different gateways)
     * @return Updated BookingResponse with Paid status
     */
    BookingResponse processPayment(Integer bookingId, Map<String, Object> paymentDetails);

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
     * Get all bookings for a customer with optional status filter.
     *
     * @param customerId The customer ID
     * @param status     Optional booking status (Held, Paid, Cancelled, Completed)
     * @return List of BookingResponse
     */
    List<BookingResponse> getBookingsByCustomerId(Integer customerId, String status);

    /**
     * Get all bookings for a trip
     *
     * @param tripId The trip ID
     * @return List of BookingResponse
     */
    List<BookingResponse> getBookingsByTripId(Integer tripId);

    /**
     * Get bookings by phone number (for guest lookup)
     *
     * @param phone The customer phone number
     * @return List of BookingResponse
     */
    List<BookingResponse> getBookingsByPhone(String phone);

    /**
     * Cancel a booking
     *
     * @param bookingId The booking ID
     * @param userId    User requesting cancellation
     * @return BookingResponse with updated status
     */
    BookingResponse cancelBooking(Integer bookingId, String userId);

    /**
     * Reschedule a booking to a new trip.
     * Cancels old booking, creates new booking, and handles refund/extra fee.
     * 
     * Policy:
     * - If new trip is cheaper: refund the difference
     * - If new trip is more expensive: customer pays extra fee
     * - Reschedule must be done at least 12 hours before old trip departure
     *
     * @param request RescheduleRequest with old booking and new trip details
     * @return RescheduleResponse with old/new booking info and financial summary
     */
    RescheduleResponse rescheduleBooking(RescheduleRequest request);
}