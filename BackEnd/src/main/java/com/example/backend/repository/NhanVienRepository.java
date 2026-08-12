package com.example.backend.repository;

import com.example.backend.entity.NhanVien;
import com.example.backend.entity.TaiKhoan;
import com.example.backend.response.NhanVienResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NhanVienRepository extends JpaRepository<NhanVien, Integer> {

    @Query("""
    SELECT new com.example.backend.response.NhanVienResponse(
        n.nhanVienId, n.hoTen, n.soDienThoai, n.email,
        cv.id, tk.username,
        n.luongCoBan, n.trangThai, n.ngayTao
    )
    FROM NhanVien n
    LEFT JOIN n.chucVu cv
    LEFT JOIN TaiKhoan tk ON tk.nhanVien = n
    WHERE cv.maChucVu <> 'admin'
    """)
    List<NhanVienResponse> hienThiNhanVien();

    @Query(value = """
        SELECT new com.example.backend.response.NhanVienResponse(
            n.nhanVienId, n.hoTen, n.soDienThoai, n.email,
            cv.id, tk.username,
            n.luongCoBan, n.trangThai, n.ngayTao
        )
        FROM NhanVien n
        LEFT JOIN n.chucVu cv
        LEFT JOIN TaiKhoan tk ON tk.nhanVien = n
        WHERE cv.maChucVu <> 'admin'
        """,
        countQuery = """
        SELECT COUNT(n) FROM NhanVien n
        LEFT JOIN n.chucVu cv
        WHERE cv.maChucVu <> 'admin'
        """)
    Page<NhanVienResponse> hienThiNhanVien(Pageable pageable);
}
