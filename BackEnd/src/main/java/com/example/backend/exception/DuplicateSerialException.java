package com.example.backend.exception;

/**
 * Throw khi serial (soSerial) đã tồn tại trong hệ thống.
 * Đánh dấu rõ ràng để GlobalExceptionHandler trả HTTP 409 CONFLICT.
 */
public class DuplicateSerialException extends IllegalArgumentException {

    public DuplicateSerialException(String soSerial) {
        super("Serial " + soSerial + " đã tồn tại trong hệ thống");
    }
}
