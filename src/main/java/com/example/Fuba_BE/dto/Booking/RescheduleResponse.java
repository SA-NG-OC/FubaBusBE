package com.example.Fuba_BE.dto.Booking;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for reschedule operation.
 * Contains old booking info, new booking info, and financial details.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response payload for reschedule operation")
public class RescheduleResponse {

    @Schema(description = "Old booking that was cancelled")
    private BookingResponse oldBooking;

    @Schema(description = "New booking that was created")
    private BookingResponse newBooking;

    @Schema(description = "Financial summary of the reschedule")
    private FinancialSummary financialSummary;

    @Schema(description = "Refund ID if refund was created")
    private Integer refundId;

    @Schema(description = "Message describing the reschedule result")
    private String message;

    /**
     * Financial summary for the reschedule operation
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Financial details of the reschedule")
    public static class FinancialSummary {

        @Schema(description = "Original booking amount", example = "500000")
        private BigDecimal oldBookingAmount;

        @Schema(description = "New booking amount", example = "600000")
        private BigDecimal newBookingAmount;

        @Schema(description = "Price difference (positive = extra fee, negative = refund)", example = "100000")
        private BigDecimal priceDifference;

        @Schema(description = "Amount to refund (if new trip is cheaper)", example = "0")
        private BigDecimal refundAmount;

        @Schema(description = "Extra fee to pay (if new trip is more expensive)", example = "100000")
        private BigDecimal extraFee;

        @Schema(description = "Reschedule fee (percentage of old booking)", example = "0")
        private BigDecimal rescheduleFee;

        @Schema(description = "Net amount (positive = customer pays, negative = customer receives)", example = "100000")
        private BigDecimal netAmount;

        @Schema(description = "Description of the financial transaction")
        private String description;
    }
}
