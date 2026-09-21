package com.example.backend.exception;

// Ngoại lệ khi serial đã bị xóa mềm
public class SerialDeletedException extends IllegalArgumentException {

    public SerialDeletedException(String message) {
        super(message);
    }
}
