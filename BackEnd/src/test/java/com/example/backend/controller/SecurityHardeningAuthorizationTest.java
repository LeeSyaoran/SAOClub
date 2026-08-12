package com.example.backend.controller;

import com.example.backend.request.BienTheSanPhamRequest;
import com.example.backend.request.DanhMucRequest;
import com.example.backend.request.KhuyenMaiRequest;
import com.example.backend.request.SanPhamRequest;
import com.example.backend.request.ThuongHieuRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

// Xác nhận các endpoint phát hiện trong đợt audit bảo mật (2026-07-21/22) đã được khoá quyền
// đúng — trước đó DashboardController/DiaChiGiaoHangController hoàn toàn không có
// @PreAuthorize (lộ doanh thu, PII địa chỉ khách hàng cho mọi role đã đăng nhập), và
// KhuyenMaiController.create/update/delete cũng vậy (ai đăng nhập cũng sửa được khuyến mãi).
// Đợt 2: SanPham/DanhMuc/ThuongHieu (permitAll không giới hạn method ở SecurityConfig —
// tạo/sửa/xoá không cần đăng nhập) và BienTheSanPham (không có @PreAuthorize — bất kỳ khách
// đăng nhập nào cũng sửa được giá bán/giá nhập).
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
    void khuyenMaiController_getAll_khongBiKhoa() throws NoSuchMethodException {
        Method m = KhuyenMaiController.class.getMethod("getAll");
        assertThat(m.getAnnotation(PreAuthorize.class)).isNull();
    }

    @Test
    void sanPhamController_createUpdateDelete_khoaChoAdminNhanVienQuanKho() throws NoSuchMethodException {
        assertThat(SanPhamController.class.getMethod("create", SanPhamRequest.class)
                .getAnnotation(PreAuthorize.class).value()).isEqualTo(STAFF_ROLES);
        assertThat(SanPhamController.class.getMethod("update", Integer.class, SanPhamRequest.class)
                .getAnnotation(PreAuthorize.class).value()).isEqualTo(STAFF_ROLES);
        assertThat(SanPhamController.class.getMethod("delete", Integer.class)
                .getAnnotation(PreAuthorize.class).value()).isEqualTo(STAFF_ROLES);
    }

    @Test
    void sanPhamController_getAll_khongBiKhoa() throws NoSuchMethodException {
        assertThat(SanPhamController.class.getMethod("getById", Integer.class)
                .getAnnotation(PreAuthorize.class)).isNull();
    }

    @Test
    void danhMucController_createUpdateDelete_khoaChoAdminNhanVienQuanKho() throws NoSuchMethodException {
        assertThat(DanhMucController.class.getMethod("create", DanhMucRequest.class)
                .getAnnotation(PreAuthorize.class).value()).isEqualTo(STAFF_ROLES);
        assertThat(DanhMucController.class.getMethod("update", Integer.class, DanhMucRequest.class)
                .getAnnotation(PreAuthorize.class).value()).isEqualTo(STAFF_ROLES);
        assertThat(DanhMucController.class.getMethod("delete", Integer.class)
                .getAnnotation(PreAuthorize.class).value()).isEqualTo(STAFF_ROLES);
    }

    @Test
    void thuongHieuController_createUpdateDelete_khoaChoAdminNhanVienQuanKho() throws NoSuchMethodException {
        assertThat(ThuongHieuController.class.getMethod("create", ThuongHieuRequest.class)
                .getAnnotation(PreAuthorize.class).value()).isEqualTo(STAFF_ROLES);
        assertThat(ThuongHieuController.class.getMethod("update", Integer.class, ThuongHieuRequest.class)
                .getAnnotation(PreAuthorize.class).value()).isEqualTo(STAFF_ROLES);
        assertThat(ThuongHieuController.class.getMethod("delete", Integer.class)
                .getAnnotation(PreAuthorize.class).value()).isEqualTo(STAFF_ROLES);
    }

    @Test
    void bienTheSanPhamController_createUpdate_khoaChoAdminNhanVienQuanKho() throws NoSuchMethodException {
        assertThat(BienTheSanPhamController.class.getMethod("create", BienTheSanPhamRequest.class)
                .getAnnotation(PreAuthorize.class).value()).isEqualTo(STAFF_ROLES);
        assertThat(BienTheSanPhamController.class.getMethod("update", Integer.class, BienTheSanPhamRequest.class)
                .getAnnotation(PreAuthorize.class).value()).isEqualTo(STAFF_ROLES);
    }
}
