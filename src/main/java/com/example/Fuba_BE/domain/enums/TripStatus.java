package com.example.Fuba_BE.domain.enums;

public enum TripStatus {
    WAITING("Waiting"),
    RUNNING("Running"),
    DELAYED("Delayed"),
    COMPLETED("Completed"),
    Cancelled("Cancelled");

    private final String displayName;

    TripStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
