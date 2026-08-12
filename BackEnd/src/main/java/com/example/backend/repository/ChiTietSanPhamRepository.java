package com.example.backend.repository;

import com.example.backend.entity.ChiTietSanPham;
import com.example.backend.response.ChiTietSanPhamResponse;
import com.example.backend.response.WarrantyStatusResponse;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChiTietSanPhamRepository extends JpaRepository<ChiTietSanPham, Integer> {
    @Query("SELECT new com.example.backend.response.ChiTietSanPhamResponse(c.chiTietId, c.bienThe.bienTheId, c.bienThe.maSku, c.soSerial, c.trangThai, c.ngayNhapKho, c.ghiChu) FROM ChiTietSanPham c")
    List<ChiTietSanPhamResponse> hienThiChiTietSanPham();

    @Query("SELECT new com.example.backend.response.ChiTietSanPhamResponse(c.chiTietId, c.bienThe.bienTheId, c.bienThe.maSku, c.soSerial, c.trangThai, c.ngayNhapKho, c.ghiChu) FROM ChiTietSanPham c WHERE c.bienThe.bienTheId = :bienTheId")
    List<ChiTietSanPhamResponse> findByBienTheId(@Param("bienTheId") Integer bienTheId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<ChiTietSanPham> findByBienThe_BienTheIdAndTrangThaiOrderByNgayNhapKhoAsc(Integer bienTheId, String trangThai);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ChiTietSanPham c WHERE c.chiTietId = :id")
    java.util.Optional<ChiTietSanPham> findByIdForUpdate(@Param("id") Integer id);

    @Query("""
    SELECT new com.example.backend.response.WarrantyStatusResponse(
        c.chiTietId, c.soSerial, bt.maSku, sp.tenSanPham, bt.baoHanhThang,
        d.ngayGiaoThucTe, d.maDonHang, kh.hoTen, kh.soDienThoai,
        d.id, bt.bienTheId, kh.khachHangId
    )
    FROM ChiTietSanPham c
    JOIN c.bienThe bt
    JOIN bt.sanPham sp
    JOIN ChiTietDonHang cdh ON cdh.chiTietSanPham = c
    JOIN cdh.donHang d
    JOIN d.khachHang kh
    WHERE c.trangThai = 'da_ban' AND d.ngayGiaoThucTe IS NOT NULL
    """)
    List<WarrantyStatusResponse> timSerialDaBanCoGiaoHang();

    boolean existsByBienThe_BienTheIdAndTrangThaiNot(Integer bienTheId, String trangThai);

    void deleteByBienThe_BienTheId(Integer bienTheId);
}
