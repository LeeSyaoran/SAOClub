package com.example.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.assertj.core.api.Assertions.assertThat;

// 5 controller nay truoc task nay KHONG co bat ky @PreAuthorize nao (mo cho moi role da
// dang nhap, ke ca khach hang). Da xac nhan (grep) chi ChiTietSanPhamService.js va
// DmService.js tung goi toi cac endpoint nay, ca 2 deu chi dung o phia admin — khoa an toan.
class DmAndSerialAuthorizationTest {

    @Test
    void dmCpuController_khoaChoAdmin() {
        PreAuthorize pa = DmCpuController.class.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasRole('ADMIN')");
    }

    @Test
    void dmRamController_khoaChoAdmin() {
        PreAuthorize pa = DmRamController.class.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasRole('ADMIN')");
    }

    @Test
    void dmGpuController_khoaChoAdmin() {
        PreAuthorize pa = DmGpuController.class.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasRole('ADMIN')");
    }

    @Test
    void dmOcungController_khoaChoAdmin() {
        PreAuthorize pa = DmOcungController.class.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasRole('ADMIN')");
    }

    @Test
    void chiTietSanPhamController_khoaChoAdminNhanVienQuanKho() {
        PreAuthorize pa = ChiTietSanPhamController.class.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')");
    }
}
