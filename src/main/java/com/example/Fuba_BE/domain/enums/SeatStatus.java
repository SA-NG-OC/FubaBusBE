package com.example.Fuba_BE.domain.enums;

/**
 * Enum representing the possible statuses of a seat.
 */
public enum SeatStatus {
    AVAILABLE("Available"),
    LOCKED("Held"),
    BOOKED("Booked");

    private final String displayName;

    SeatStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Converts a display name string to the corresponding SeatStatus enum.
     * @param displayName The Vietnamese display name
     * @return The corresponding SeatStatus enum
     */
    public static SeatStatus fromDisplayName(String displayName) {
        for (SeatStatus status : values()) {
            if (status.displayName.equals(displayName)) {
                return status;
            }
        }
        return AVAILABLE; // Default to AVAILABLE if not found
    }
}
