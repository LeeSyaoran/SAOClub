package com.example.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.assertj.core.api.Assertions.assertThat;

// PhieuBaoHanhController KHÔNG có bất kỳ @PreAuthorize nào trước task này (mở cho mọi
// role đã đăng nhập, kể cả khách hàng). Đã xác nhận (grep) chưa có luồng khách hàng nào
// gọi tới endpoint này nên khoá an toàn tuyệt đối, đúng tiền lệ PhieuTraHangController.
class PhieuBaoHanhAuthorizationTest {

    @Test
    void phieuBaoHanhController_khoaChoAdminNhanVienQuanKho() {
        PreAuthorize pa = PhieuBaoHanhController.class.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')");
    }
}
