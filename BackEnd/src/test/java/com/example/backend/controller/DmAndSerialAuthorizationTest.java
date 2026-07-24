package com.example.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.assertj.core.api.Assertions.assertThat;

// 4 controller Dm* khoa cho ADMIN/NHAN_VIEN/QUAN_KHO (truoc day chi ADMIN — qua hep, staff
// khong CRUD duoc CPU/RAM/GPU/O cung o trang Kho hang). ChiTietSanPhamController giu nguyen.
class DmAndSerialAuthorizationTest {

    @Test
    void dmCpuController_khoaChoAdmin() {
        PreAuthorize pa = DmCpuController.class.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')");
    }

    @Test
    void dmRamController_khoaChoAdmin() {
        PreAuthorize pa = DmRamController.class.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')");
    }

    @Test
    void dmGpuController_khoaChoAdmin() {
        PreAuthorize pa = DmGpuController.class.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')");
    }

    @Test
    void dmOcungController_khoaChoAdmin() {
        PreAuthorize pa = DmOcungController.class.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')");
    }

    @Test
    void chiTietSanPhamController_khoaChoAdminNhanVienQuanKho() {
        PreAuthorize pa = ChiTietSanPhamController.class.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')");
    }
}
