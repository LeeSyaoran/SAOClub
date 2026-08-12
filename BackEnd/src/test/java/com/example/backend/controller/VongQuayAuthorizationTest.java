package com.example.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class VongQuayAuthorizationTest {

    @Test
    void controller_khongCoPreAuthorizeCapClass() {
        assertThat(VongQuayController.class.getAnnotation(PreAuthorize.class)).isNull();
    }

    @Test
    void capNhatCauHinh_chiChoAdminNhanVienQuanKho() throws NoSuchMethodException {
        Method m = VongQuayController.class.getMethod("capNhatCauHinh",
                com.example.backend.request.CauHinhVongQuayRequest.class);
        PreAuthorize pa = m.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')");
    }

    @Test
    void quay_khongCoPreAuthorizeRieng() throws NoSuchMethodException {
        Method m = VongQuayController.class.getMethod("quay");
        assertThat(m.getAnnotation(PreAuthorize.class)).isNull();
    }

    @Test
    void getCauHinh_khongCoPreAuthorizeRieng() throws NoSuchMethodException {
        Method m = VongQuayController.class.getMethod("getCauHinh");
        assertThat(m.getAnnotation(PreAuthorize.class)).isNull();
    }
}
