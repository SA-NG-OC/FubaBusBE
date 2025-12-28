package com.example.Fuba_BE.domain.enums;

public enum StopType {

    ORIGIN("Điểm khởi hành"),
    INTERMEDIATE("Điểm dừng chân"),
    DESTINATION("Điểm đến");

    private final String dbValue;

    StopType(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }
}
