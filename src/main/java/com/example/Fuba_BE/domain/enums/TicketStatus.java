package com.example.Fuba_BE.domain.enums;

/**
 * Enum representing the possible statuses of a ticket.
 */
public enum TicketStatus {
    UNCONFIRMED("Unconfirmed"),
    CONFIRMED("Confirmed"),
    CHECKED_IN("CheckedIn"),
    USED("Used"),
    CANCELLED("Cancelled"),
    NO_SHOW("NoShow"),
    RESCHEDULED("Rescheduled");

    private final String displayName;

    TicketStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Converts a display name string to the corresponding TicketStatus enum.
     * 
     * @param displayName The display name (e.g., "Confirmed", "CheckedIn")
     * @return The corresponding TicketStatus enum
     * @throws IllegalArgumentException if no matching status found
     */
    public static TicketStatus fromDisplayName(String displayName) {
        for (TicketStatus status : values()) {
            if (status.displayName.equalsIgnoreCase(displayName)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown ticket status: " + displayName);
    }
}
