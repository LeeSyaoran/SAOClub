package com.example.backend.exception;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.EntityNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handlerValidateErrors(MethodArgumentNotValidException e) {
        Map<String, String> errorMap = new LinkedHashMap<>();
        List<String> messages = new ArrayList<>();
        e.getBindingResult().getFieldErrors().forEach(s -> {
            errorMap.put(s.getField(), s.getDefaultMessage());
            if (s.getDefaultMessage() != null && !messages.contains(s.getDefaultMessage())) {
                messages.add(s.getDefaultMessage());
            }
        });
        if (!messages.isEmpty()) {
            errorMap.put("message", String.join("; ", messages));
        }
        return new ResponseEntity<>(errorMap, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handlerMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("Invalid request body format: {}", e.getMessage());
        String raw = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage();
        String msg = "Dữ liệu gửi lên không đúng định dạng hoặc số vượt quá giới hạn cho phép (vui lòng kiểm tra lại Trọng lượng, Giá, Số lượng)";
        if (raw != null && raw.toLowerCase().contains("trongluongkg")) {
            msg = "Trọng lượng (kg) không hợp lệ hoặc vượt quá giới hạn cho phép (tối đa 999.99 kg)";
        }
        return new ResponseEntity<>(msg, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DuplicateSerialException.class)
    public ResponseEntity<?> handlerDuplicateSerial(DuplicateSerialException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(SerialDeletedException.class)
    public ResponseEntity<?> handlerSerialDeleted(SerialDeletedException e) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("code", "DELETED");
        body.put("message", e.getMessage());
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({IllegalArgumentException.class, UsernameNotFoundException.class, IllegalStateException.class})
    public ResponseEntity<?> handlerBusinessErrors(RuntimeException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handlerAccessDenied(AccessDeniedException e) {
        return new ResponseEntity<>("Bạn không có quyền thực hiện thao tác này", HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<?> handlerEntityNotFound(EntityNotFoundException e) {
        log.warn("Entity not found: {}", e.getMessage());
        Map<String, String> body = new LinkedHashMap<>();
        body.put("code", "NOT_FOUND");
        String msg = e.getMessage();
        if (msg != null && !msg.isBlank()) {
            body.put("message", msg);
        } else {
            body.put("message", "Dữ liệu không hợp lệ hoặc liên kết không tồn tại, vui lòng kiểm tra lại");
        }
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handlerDataIntegrity(DataIntegrityViolationException e) {
        String rootMsg = (e.getMostSpecificCause() != null && e.getMostSpecificCause().getMessage() != null)
                ? e.getMostSpecificCause().getMessage()
                : (e.getMessage() != null ? e.getMessage() : "");
        log.warn("Data integrity violation: {}", rootMsg);
        String lower = rootMsg.toLowerCase();
        String msg;
        if (lower.contains("arithmetic overflow") || lower.contains("numeric")) {
            msg = "Giá trị số vượt quá giới hạn cho phép (ví dụ: Trọng lượng máy tính tối đa 5 kg, hoặc Giá bán/Giá vốn vượt quá giới hạn). Vui lòng kiểm tra lại!";
        } else if (lower.contains("truncated") || lower.contains("string or binary data")) {
            msg = "Độ dài nội dung nhập vào vượt quá giới hạn cho phép của hệ thống, vui lòng rút gọn lại.";
        } else if (lower.contains("ck_bt_giaban_hop_ly")) {
            msg = "Giá bán không hợp lệ (phải lớn hơn hoặc bằng 50% giá vốn).";
        } else if (lower.contains("ck_sp_loaisanpham")) {
            msg = "Loại sản phẩm chỉ nhận LAPTOP, PHU_KIEN hoặc DIEN_THOAI.";
        } else if (lower.contains("ck_ctpn_soluong") || lower.contains("ck_ctdh_soluong") || lower.contains("ck_ctth_soluong")) {
            msg = "Số lượng sản phẩm trong phiếu phải lớn hơn 0.";
        } else if (lower.contains("ck_chtk_giu_le_ton") || lower.contains("ck_chtk_tonthucte")) {
            msg = "Số lượng tồn kho không hợp lệ hoặc vượt quá số lượng khả dụng.";
        } else if (lower.contains("serial")) {
            msg = "Serial này đã tồn tại trong hệ thống, vui lòng kiểm tra lại";
        } else if (lower.contains("ma_sku")) {
            msg = "Mã SKU đã tồn tại ở phiên bản khác, vui lòng kiểm tra lại";
        } else if (lower.contains("barcode")) {
            msg = "Mã vạch (Barcode) đã tồn tại ở sản phẩm/phiên bản khác, vui lòng kiểm tra lại";
        } else if (lower.contains("ma_san_pham") || lower.contains("ux_san_pham_ma")) {
            msg = "Mã sản phẩm đã tồn tại trong hệ thống, vui lòng kiểm tra lại";
        } else if (lower.contains("ten_thuong_hieu")) {
            msg = "Tên thương hiệu này đã tồn tại";
        } else if (lower.contains("ten_danh_muc")) {
            msg = "Tên danh mục này đã tồn tại";
        } else if (lower.contains("so_dien_thoai")) {
            msg = "Số điện thoại này đã được sử dụng";
        } else if (lower.contains("email")) {
            msg = "Email này đã được sử dụng";
        } else if (lower.contains("reference constraint") || lower.contains("foreign key") || lower.contains("fk_")) {
            msg = "Không thể thực hiện thao tác do dữ liệu đang được liên kết ở phiếu/đơn khác hoặc danh mục tham chiếu không tồn tại";
        } else {
            msg = "Dữ liệu vi phạm ràng buộc hệ thống" + (!rootMsg.isBlank() ? ": " + rootMsg : ", vui lòng kiểm tra lại");
        }
        return new ResponseEntity<>(msg, HttpStatus.CONFLICT);
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

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<?> handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handlerUnexpected(Exception e) {
        log.error("Unhandled exception", e);
        String detail = e.getMessage() != null && !e.getMessage().isBlank() ? ": " + e.getMessage() : ", vui lòng thử lại sau";
        return new ResponseEntity<>("Đã có lỗi xảy ra" + detail, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
