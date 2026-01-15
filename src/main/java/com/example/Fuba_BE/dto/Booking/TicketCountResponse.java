package com.example.Fuba_BE.dto.Booking;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for ticket count by status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Count of tickets grouped by status")
public class TicketCountResponse {

    @Schema(description = "Number of upcoming bookings", example = "2")
    private Long upcomingCount;

    @Schema(description = "Number of completed bookings", example = "1")
    private Long completedCount;

    @Schema(description = "Number of cancelled bookings", example = "1")
    private Long cancelledCount;

    @Schema(description = "Total number of bookings", example = "4")
    private Long totalCount;
}
