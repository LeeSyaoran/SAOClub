package com.example.backend.repository;

import com.example.backend.entity.KhachHang;
import com.example.backend.response.KhachHangResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KhachHangRepository extends JpaRepository<KhachHang,Integer> {

    Optional<KhachHang> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsBySoDienThoai(String soDienThoai);
    @Query("""
        SELECT new com.example.backend.response.KhachHangResponse(
            k.khachHangId,
            k.hoTen,
            k.soDienThoai,
            k.email,
            k.diaChi,
            k.loaiKhach,
            k.tenCongTy,
            k.maSoThue,
            k.diemTichLuy,
            k.trangThai,
            k.ngayTao)
        FROM KhachHang k
    """)
    List<KhachHangResponse> hienThiKhachHang();

}