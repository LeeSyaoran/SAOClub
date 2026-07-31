package com.example.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

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

    // NhanVienController: sua/xoa van chi ADMIN (class-level), nhung GET (danh sach nhan
    // vien de chon/hien thi o ReturnsPanel.vue, phieu nhap kho) phai mo cho NHAN_VIEN/QUAN_KHO
    // qua override o method-level — truoc day GET cung bi khoa ADMIN-only lam quan_kho
    // khong tao duoc phieu nhap (khong chon duoc Staff).
    @Test
    void nhanVienController_classLevelVanChiAdmin() {
        PreAuthorize pa = NhanVienController.class.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasRole('ADMIN')");
    }

    @Test
    void nhanVienController_getAll_moChoNhanVienQuanKho() throws NoSuchMethodException {
        // getAll() có phân trang (page, size) — xem NhanVienController.java
        Method m = NhanVienController.class.getMethod("getAll", int.class, int.class);
        PreAuthorize pa = m.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')");
    }

    @Test
    void nhanVienController_getById_moChoNhanVienQuanKho() throws NoSuchMethodException {
        Method m = NhanVienController.class.getMethod("getById", Integer.class);
        PreAuthorize pa = m.getAnnotation(PreAuthorize.class);
        assertThat(pa).isNotNull();
        assertThat(pa.value()).isEqualTo("hasAnyRole('ADMIN','NHAN_VIEN','QUAN_KHO')");
    }
}
