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

    // Lỗi validate @Valid trên request body
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handlerValidateErrors(MethodArgumentNotValidException e) {
        Map<String, String> errorMap = new HashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(s -> errorMap.put(s.getField(), s.getDefaultMessage()));
        return new ResponseEntity<>(errorMap, HttpStatus.BAD_REQUEST);
    }

    // Lỗi nghiệp vụ tự ném (không tồn tại, trùng dữ liệu, trạng thái không hợp lệ...) — message viết sẵn, an toàn để hiển thị
    @ExceptionHandler({IllegalArgumentException.class, UsernameNotFoundException.class, IllegalStateException.class})
    public ResponseEntity<?> handlerBusinessErrors(RuntimeException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    // Không đủ quyền
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handlerAccessDenied(AccessDeniedException e) {
        return new ResponseEntity<>("Bạn không có quyền thực hiện thao tác này", HttpStatus.FORBIDDEN);
    }

    // ID liên kết (FK) không tồn tại — có thể ném ra khi getReferenceById() được
    // truy cập (EntityNotFoundException) hoặc khi DB từ chối lúc insert vì khoá
    // ngoại/ràng buộc không hợp lệ (DataIntegrityViolationException). Cả hai đều
    // xảy ra phổ biến ở các service dùng getReferenceById() để gán quan hệ mà
    // không load cả entity — bắt tập trung ở đây thay vì existsById() thủ công
    // ở từng chỗ gọi (~70 chỗ trong 19 service).
    @ExceptionHandler({EntityNotFoundException.class, DataIntegrityViolationException.class})
    public ResponseEntity<?> handlerEntityNotFound(Exception e) {
        log.warn("Data integrity error: {}", e.getMessage());
        return new ResponseEntity<>("Dữ liệu không hợp lệ hoặc liên kết không tồn tại, vui lòng kiểm tra lại", HttpStatus.BAD_REQUEST);
    }

    // Gọi sai phương thức HTTP (vd GET vào endpoint chỉ nhận POST) — lỗi phía client,
    // không phải lỗi server: trả đúng 405 thay vì rơi xuống handlerUnexpected() bên dưới
    // (log ERROR kèm stack trace như một lỗi thật, trả nhầm 500).
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<?> handlerMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.debug("Method not supported: {}", e.getMessage());
        return new ResponseEntity<>("Phương thức không được hỗ trợ cho endpoint này", HttpStatus.METHOD_NOT_ALLOWED);
    }

    // Client (trình duyệt) đã ngắt kết nối giữa chừng khi server đang ghi response
    // (reload trang, đóng tab, Vite HMR...) — không phải lỗi server, không viết body
    // (kết nối đã chết, viết cũng sẽ thất bại lần nữa) và chỉ log nhẹ để khỏi rác log.
    @ExceptionHandler(HttpMessageNotWritableException.class)
    public void handlerClientDisconnected(HttpMessageNotWritableException e) {
        log.debug("Client disconnected before response could be written: {}", e.getMessage());
    }

    // Lỗi không lường trước (SQL, NPE...) — log chi tiết ở server, không lộ ra ngoài
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handlerUnexpected(Exception e) {
        log.error("Unhandled exception", e);
        return new ResponseEntity<>("Đã có lỗi xảy ra, vui lòng thử lại sau", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
