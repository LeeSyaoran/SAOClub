package com.example.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.assertj.core.api.Assertions.assertThat;

// Xác nhận PhieuTraHangController/ChiTietTraHangController bị khoá quyền — trước khi có
// UI (Task 5-10) gọi tới, 2 controller này KHÔNG có bất kỳ @PreAuthorize nào (mở cho mọi
// role đã đăng nhập, kể cả khách hàng). Đã xác nhận (grep) chưa có luồng khách hàng nào
// gọi 2 endpoint này nên khoá an toàn tuyệt đối, đúng tiền lệ NhaCungCapController/TonKhoController.
class PhieuTraHangAuthorizationTest {

    @Test
    void phieuTraHangController_khoaChoAdminNhanVienQuanKho() {
        PreAuthorize pa = PhieuTraHangController.class.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')");
    }

    @Test
    void chiTietTraHangController_khoaChoAdminNhanVienQuanKho() {
        PreAuthorize pa = ChiTietTraHangController.class.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')");
    }
}
