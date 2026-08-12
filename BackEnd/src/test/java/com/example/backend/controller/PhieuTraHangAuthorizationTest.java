package com.example.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.assertj.core.api.Assertions.assertThat;

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
