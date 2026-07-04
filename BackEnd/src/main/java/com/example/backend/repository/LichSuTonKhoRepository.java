package com.example.backend.repository;

import com.example.backend.entity.LichSuTonKho;
import com.example.backend.response.LichSuTonKhoResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LichSuTonKhoRepository extends JpaRepository<LichSuTonKho, Integer> {
    @Query("SELECT new com.example.backend.response.LichSuTonKhoResponse(l.lichSuId, l.bienThe.bienTheId, l.bienThe.maSku, l.chiTietSanPham.chiTietId, l.loaiBienDong, l.soLuongThayDoi, l.donHang.id, l.phieuNhapKho.phieuNhapId, l.nhanVien.nhanVienId, l.ghiChu, l.ngayTao) FROM LichSuTonKho l")
    List<LichSuTonKhoResponse> hienThiLichSuTonKho();

    List<LichSuTonKho> findByDonHang_Id(Integer donHangId);
}
