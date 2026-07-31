package com.example.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

// Không có tiền lệ test @WebMvcTest/MockMvc trong dự án (mọi test hiện tại là
// unit test tầng service với Mockito) và spring-security-test chưa phải dependency
// của module — dựng bộ chạy HTTP thật cho riêng test này là hạ tầng mới, không cần thiết.
// Test này xác nhận trực tiếp bằng reflection rằng @PreAuthorize("hasRole('ADMIN')")
// (đúng cú pháp đã dùng ở ChucVuController/NhanVienController) có mặt trên
// update/apDungNguongTonKho, và KHÔNG có trên get/doiMatKhau — get() là GET /api/cai-dat,
// đã permitAll ở SecurityConfig (khách chưa đăng nhập vẫn xem được cấu hình cửa hàng).
class CaiDatControllerAuthorizationTest {

    private static PreAuthorize preAuthorizeOf(String methodName) throws NoSuchMethodException {
        Method m = switch (methodName) {
            case "get" -> CaiDatController.class.getMethod("get");
            case "update" -> CaiDatController.class.getMethod("update", com.example.backend.request.CaiDatHeThongRequest.class);
            case "apDungNguongTonKho" -> CaiDatController.class.getMethod("apDungNguongTonKho", java.util.Map.class);
            case "doiMatKhau" -> CaiDatController.class.getMethod("doiMatKhau", com.example.backend.request.DoiMatKhauRequest.class);
            default -> throw new IllegalArgumentException(methodName);
        };
        return m.getAnnotation(PreAuthorize.class);
    }

    @Test
    void get_khongBiGioiHanRole() throws Exception {
        // GET /api/cai-dat đã permitAll ở SecurityConfig — không được thêm @PreAuthorize
        // giới hạn role ở đây, nếu không khách chưa đăng nhập sẽ bị 403 dù URL cho phép qua.
        assertThat(preAuthorizeOf("get")).isNull();
    }

    @Test
    void update_bienCoAnnotationChiAdmin() throws Exception {
        PreAuthorize pa = preAuthorizeOf("update");
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasRole('ADMIN')");
    }

    @Test
    void apDungNguongTonKho_bienCoAnnotationChiAdmin() throws Exception {
        PreAuthorize pa = preAuthorizeOf("apDungNguongTonKho");
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasRole('ADMIN')");
    }

    @Test
    void doiMatKhau_khongBiGioiHanRole() throws Exception {
        // doi-mat-khau phải mở cho MỌI role đã đăng nhập (admin, nhan_vien, quan_kho, khach_hang)
        // — không được thêm @PreAuthorize giới hạn role ở đây.
        assertThat(preAuthorizeOf("doiMatKhau")).isNull();
    }
}
