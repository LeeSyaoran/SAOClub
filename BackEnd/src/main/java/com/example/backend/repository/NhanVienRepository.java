package com.example.backend.repository;

import com.example.backend.entity.NhanVien;
import com.example.backend.response.NhanVienResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NhanVienRepository extends JpaRepository<NhanVien, Integer> {

    // === BUG CŨ ===
    // LEFT JOIN n.chucVu (không có alias) → alias không tồn tại trong SELECT
    // → n.chucVu.id trong SELECT tạo ra IMPLICIT INNER JOIN thứ hai
    // → Nhân viên không có chức vụ (chuc_vu_id IS NULL) bị loại khỏi kết quả
    //
    // === FIX ===
    // Thêm alias "cv" vào LEFT JOIN, dùng cv.id trong SELECT
    // Kết quả: nhân viên không có chức vụ vẫn xuất hiện, cv.id = null
    @Query("""
    SELECT new com.example.backend.response.NhanVienResponse(
        n.nhanVienId, n.hoTen, n.soDienThoai, n.email,
        cv.id,
        n.username, n.luongCoBan, n.trangThai, n.ngayTao
    )
    FROM NhanVien n
    LEFT JOIN n.chucVu cv
    """)
    List<NhanVienResponse> hienThiNhanVien();
}
