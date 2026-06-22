package com.example.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

// @RestControllerAdvice = bắt exception từ TẤT CẢ controller, tự động trả JSON thay vì HTML error page
// Không cần try-catch trong từng controller — exception tự "nổi" lên đây
@RestControllerAdvice
public class GlobalExceptionHandler {

    // IllegalArgumentException: dùng khi không tìm thấy entity (findById().orElseThrow)
    // → trả 404 Not Found thay vì 500 Internal Server Error
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(IllegalArgumentException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // MethodArgumentNotValidException: do @Valid trong controller gặp vi phạm validation annotation
    // (@NotBlank, @NotNull, @PositiveOrZero trong Request DTO)
    // → trả 400 Bad Request, body là map { "fieldName": "thông báo lỗi" }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    // Exception tổng quát: bắt mọi lỗi chưa được xử lý ở trên
    // → trả 500 Internal Server Error, tránh lộ stack trace ra client
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception ex) {
        Map<String, String> body = new HashMap<>();
        body.put("error", "Lỗi hệ thống: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
