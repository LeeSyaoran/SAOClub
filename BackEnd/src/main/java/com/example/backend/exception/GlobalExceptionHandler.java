package com.example.backend.exception;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handlerValidateErrors(MethodArgumentNotValidException e) {
        Map<String, String> errorMap = new HashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(s -> errorMap.put(s.getField(), s.getDefaultMessage()));
        return new ResponseEntity<>(errorMap, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({IllegalArgumentException.class, UsernameNotFoundException.class, IllegalStateException.class})
    public ResponseEntity<?> handlerBusinessErrors(RuntimeException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handlerAccessDenied(AccessDeniedException e) {
        return new ResponseEntity<>("Bạn không có quyền thực hiện thao tác này", HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler({EntityNotFoundException.class, DataIntegrityViolationException.class})
    public ResponseEntity<?> handlerEntityNotFound(Exception e) {
        log.warn("Data integrity error: {}", e.getMessage());
        return new ResponseEntity<>("Dữ liệu không hợp lệ hoặc liên kết không tồn tại, vui lòng kiểm tra lại", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<?> handlerMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.debug("Method not supported: {}", e.getMessage());
        return new ResponseEntity<>("Phương thức không được hỗ trợ cho endpoint này", HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(HttpMessageNotWritableException.class)
    public void handlerClientDisconnected(HttpMessageNotWritableException e) {
        log.debug("Client disconnected before response could be written: {}", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handlerUnexpected(Exception e) {
        log.error("Unhandled exception", e);
        return new ResponseEntity<>("Đã có lỗi xảy ra, vui lòng thử lại sau", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
