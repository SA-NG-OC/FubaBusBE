package com.example.Fuba_BE.dto.Booking;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for paginated booking list.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Paginated response for bookings")
public class BookingPageResponse {

    @Schema(description = "List of bookings in current page")
    private List<BookingResponse> bookings;

    @Schema(description = "Current page number (0-indexed)", example = "0")
    private Integer currentPage;

    @Schema(description = "Page size", example = "20")
    private Integer pageSize;

    @Schema(description = "Total number of bookings", example = "150")
    private Long totalElements;

    @Schema(description = "Total number of pages", example = "8")
    private Integer totalPages;

    @Schema(description = "Is first page", example = "true")
    private Boolean isFirst;

    @Schema(description = "Is last page", example = "false")
    private Boolean isLast;
}
