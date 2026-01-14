package com.example.Fuba_BE.dto.Booking;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for filtering and paginating bookings.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request parameters for filtering and paginating bookings")
public class BookingFilterRequest {

    @Schema(description = "Page number (0-indexed)", example = "0", defaultValue = "0")
    @Min(0)
    @Builder.Default
    private Integer page = 0;

    @Schema(description = "Page size", example = "20", defaultValue = "20")
    @Min(1)
    @Builder.Default
    private Integer size = 20;

    @Schema(description = "Booking status filter", example = "Paid", allowableValues = {"Held", "Pending", "Paid", "Completed", "Cancelled", "Expired"})
    private String status;

    @Schema(description = "Search by booking code, customer name, or phone", example = "BK20260105001")
    private String search;

    @Schema(description = "Sort by field", example = "createdAt", defaultValue = "createdAt", allowableValues = {"createdAt", "totalAmount", "bookingCode"})
    @Builder.Default
    private String sortBy = "createdAt";

    @Schema(description = "Sort direction", example = "DESC", defaultValue = "DESC", allowableValues = {"ASC", "DESC"})
    @Builder.Default
    private String sortDirection = "DESC";
}
