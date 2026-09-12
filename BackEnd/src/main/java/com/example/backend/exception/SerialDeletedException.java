package com.example.backend.exception;

/**
 * Throw khi barcode/serial da bi xoa mem khoi he thong.
 * GlobalExceptionHandler tra HTTP 404 voi body {code: "DELETED", message: ...}
 */
public class SerialDeletedException extends IllegalArgumentException {

    public SerialDeletedException(String message) {
        super(message);
    }
}
