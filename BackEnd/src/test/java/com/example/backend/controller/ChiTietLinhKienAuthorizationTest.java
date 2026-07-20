package com.example.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.assertj.core.api.Assertions.assertThat;

// 4 controller nay la moi hoan toan (Task 1 cua plan tai cau truc serial linh kien) —
// khoa dung khuon ChiTietSanPhamController vi Kho can them serial linh kien qua
// SerialManager cho spec da co san.
class ChiTietLinhKienAuthorizationTest {

    @Test
    void chiTietCpuController_khoaChoAdminNhanVienQuanKho() {
        PreAuthorize pa = ChiTietCpuController.class.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')");
    }

    @Test
    void chiTietRamController_khoaChoAdminNhanVienQuanKho() {
        PreAuthorize pa = ChiTietRamController.class.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')");
    }

    @Test
    void chiTietGpuController_khoaChoAdminNhanVienQuanKho() {
        PreAuthorize pa = ChiTietGpuController.class.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')");
    }

    @Test
    void chiTietOcungController_khoaChoAdminNhanVienQuanKho() {
        PreAuthorize pa = ChiTietOcungController.class.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')");
    }
}
