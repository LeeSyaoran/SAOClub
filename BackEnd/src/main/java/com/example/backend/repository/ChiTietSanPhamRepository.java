package com.example.backend.repository;

import com.example.backend.entity.ChiTietSanPham;
import com.example.backend.response.ChiTietSanPhamResponse;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChiTietSanPhamRepository extends JpaRepository<ChiTietSanPham, Integer> {
    @Query("SELECT new com.example.backend.response.ChiTietSanPhamResponse(c.chiTietId, c.bienThe.bienTheId, c.bienThe.maSku, c.soSerial, c.trangThai, c.ngayNhapKho) FROM ChiTietSanPham c")
    List<ChiTietSanPhamResponse> hienThiChiTietSanPham();

    @Query("SELECT new com.example.backend.response.ChiTietSanPhamResponse(c.chiTietId, c.bienThe.bienTheId, c.bienThe.maSku, c.soSerial, c.trangThai, c.ngayNhapKho) FROM ChiTietSanPham c WHERE c.bienThe.bienTheId = :bienTheId")
    List<ChiTietSanPhamResponse> findByBienTheId(@Param("bienTheId") Integer bienTheId);

    // Lay cac serial con trong kho (FIFO) de tu dong gan khi ban hang — nhap truoc xuat truoc.
    // PESSIMISTIC_WRITE (SELECT ... FOR UPDATE): khoa cac dong nay lai trong pham vi transaction
    // cua caller, tranh 2 don hang cung luc gianh cung 1 serial.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<ChiTietSanPham> findByBienThe_BienTheIdAndTrangThaiOrderByNgayNhapKhoAsc(Integer bienTheId, String trangThai);
}
