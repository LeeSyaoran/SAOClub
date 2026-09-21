package com.example.backend.exception;

// Ngoại lệ khi trùng số serial
public class DuplicateSerialException extends IllegalArgumentException {

    public DuplicateSerialException(String soSerial) {
        super("Serial " + soSerial + " đã tồn tại trong hệ thống");
    }
}
