package com.example.backend.repository;

import com.example.backend.entity.TaiKhoan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaiKhoanRepository extends JpaRepository<TaiKhoan, Integer> {
    Optional<TaiKhoan> findByUsername(String username);
    boolean existsByUsername(String username);
    Optional<TaiKhoan> findByNhanVien_NhanVienId(Integer nhanVienId);
    Optional<TaiKhoan> findByKhachHang_KhachHangId(Integer khachHangId);
}
