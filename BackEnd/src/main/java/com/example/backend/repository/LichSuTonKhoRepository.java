package com.example.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.backend.entity.LichSuTonKho;
import com.example.backend.response.LichSuTonKhoResponse;

@Repository
public interface LichSuTonKhoRepository extends JpaRepository<LichSuTonKho, Integer> {
    @Query("SELECT new com.example.backend.response.LichSuTonKhoResponse(" +
           "l.lichSuId, l.bienThe.bienTheId, l.bienThe.maSku, l.chiTietSanPham.chiTietId, " +
           "l.loaiBienDong, l.soLuongThayDoi, l.donHang.id, l.phieuNhapKho.phieuNhapId, " +
           "l.nhanVien.nhanVienId, l.ghiChu, l.ngayTao, " +
           "l.bienThe.sanPham.tenSanPham, l.nhanVien.hoTen) " +
           "FROM LichSuTonKho l " +
           "LEFT JOIN l.donHang " +
           "LEFT JOIN l.phieuNhapKho " +
           "LEFT JOIN l.nhanVien " +
           "LEFT JOIN l.chiTietSanPham " +
           "LEFT JOIN l.bienThe.sanPham")
    List<LichSuTonKhoResponse> hienThiLichSuTonKho();

    List<LichSuTonKho> findByDonHang_Id(Integer donHangId);

    void deleteByBienThe_BienTheId(Integer bienTheId);
}
