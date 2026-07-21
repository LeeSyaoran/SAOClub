package com.example.backend.controller;

import com.example.backend.request.KhuyenMaiRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

// Xác nhận các endpoint phát hiện trong đợt audit bảo mật (2026-07-21) đã được khoá quyền
// đúng — trước đó DashboardController/DiaChiGiaoHangController hoàn toàn không có
// @PreAuthorize (lộ doanh thu, PII địa chỉ khách hàng cho mọi role đã đăng nhập), và
// KhuyenMaiController.create/update/delete cũng vậy (ai đăng nhập cũng sửa được khuyến mãi).
class SecurityHardeningAuthorizationTest {

    private static final String STAFF_ROLES = "hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')";

    @Test
    void dashboardController_khoaChoAdminNhanVienQuanKho() {
        PreAuthorize pa = DashboardController.class.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo(STAFF_ROLES);
    }

    @Test
    void diaChiGiaoHangController_khoaChoAdminNhanVienQuanKho() {
        PreAuthorize pa = DiaChiGiaoHangController.class.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo(STAFF_ROLES);
    }

    @Test
    void uploadController_khoaChoAdminNhanVienQuanKho() {
        PreAuthorize pa = UploadController.class.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo(STAFF_ROLES);
    }

    // KhuyenMaiController.getAll()/getById() PHẢI giữ mở (CheckoutModal.vue tải mã khuyến
    // mãi cho cả khách vãng lai chưa đăng nhập) — chỉ create/update/delete bị khoá riêng
    // từng method, không khoá ở class-level.
    @Test
    void khuyenMaiController_khongCoKhoaClassLevel() {
        assertThat(KhuyenMaiController.class.getAnnotation(PreAuthorize.class)).isNull();
    }

    @Test
    void khuyenMaiController_create_khoaChoAdminNhanVienQuanKho() throws NoSuchMethodException {
        Method m = KhuyenMaiController.class.getMethod("create", KhuyenMaiRequest.class);
        PreAuthorize pa = m.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo(STAFF_ROLES);
    }

    @Test
    void khuyenMaiController_update_khoaChoAdminNhanVienQuanKho() throws NoSuchMethodException {
        Method m = KhuyenMaiController.class.getMethod("update", Integer.class, KhuyenMaiRequest.class);
        PreAuthorize pa = m.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo(STAFF_ROLES);
    }

    @Test
    void khuyenMaiController_delete_khoaChoAdminNhanVienQuanKho() throws NoSuchMethodException {
        Method m = KhuyenMaiController.class.getMethod("delete", Integer.class);
        PreAuthorize pa = m.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo(STAFF_ROLES);
    }

    @Test
    void khuyenMaiController_getAll_khongBiKhoa() throws NoSuchMethodException {
        Method m = KhuyenMaiController.class.getMethod("getAll");
        assertThat(m.getAnnotation(PreAuthorize.class)).isNull();
    }
}
