package com.example.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(preAuthorizeOf("doiMatKhau")).isNull();
    }
}
