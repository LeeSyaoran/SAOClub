package com.example.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.assertj.core.api.Assertions.assertThat;

class PhieuBaoHanhAuthorizationTest {

    @Test
    void phieuBaoHanhController_khoaChoAdminNhanVienQuanKho() {
        PreAuthorize pa = PhieuBaoHanhController.class.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')");
    }
}
