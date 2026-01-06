package com.example.Fuba_BE.domain.enums;

/**
 * Enum representing the possible statuses of a seat.
 * Values match database constraints: Available, Held, Booked
 */
public enum SeatStatus {
    Available("Available"),
    Held("Held"),
    Booked("Booked");

    private final String displayName;

    SeatStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Converts a display name string to the corresponding SeatStatus enum.
     * @param displayName The status name
     * @return The corresponding SeatStatus enum
     */
    public static SeatStatus fromDisplayName(String displayName) {
        for (SeatStatus status : values()) {
            if (status.displayName.equals(displayName)) {
                return status;
            }
        }
        return Available; // Default to AVAILABLE if not found
    }
}
