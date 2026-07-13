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
    @Query("SELECT new com.example.backend.response.ChiTietSanPhamResponse(c.chiTietId, c.bienThe.bienTheId, c.bienThe.maSku, c.soSerial, c.trangThai, c.ngayNhapKho) FROM ChiTietSanPham c")
    List<ChiTietSanPhamResponse> hienThiChiTietSanPham();

    @Query("SELECT new com.example.backend.response.ChiTietSanPhamResponse(c.chiTietId, c.bienThe.bienTheId, c.bienThe.maSku, c.soSerial, c.trangThai, c.ngayNhapKho) FROM ChiTietSanPham c WHERE c.bienThe.bienTheId = :bienTheId")
    List<ChiTietSanPhamResponse> findByBienTheId(@Param("bienTheId") Integer bienTheId);

    // Lay cac serial con trong kho (FIFO) de tu dong gan khi ban hang — nhap truoc xuat truoc.
    // PESSIMISTIC_WRITE (SELECT ... FOR UPDATE): khoa cac dong nay lai trong pham vi transaction
    // cua caller, tranh 2 don hang cung luc gianh cung 1 serial.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<ChiTietSanPham> findByBienThe_BienTheIdAndTrangThaiOrderByNgayNhapKhoAsc(Integer bienTheId, String trangThai);

    // Serial đã bán, kèm ngày giao thực tế (thời điểm bắt đầu tính bảo hành) + số tháng bảo
    // hành của biến thể — ngày hết bảo hành tính ở service (ngayGiaoThucTe + baoHanhThang).
    // Chỉ lấy đơn đã có ngayGiaoThucTe (đã giao tới tay khách) — đơn chưa giao thì bảo hành
    // chưa bắt đầu, không đưa vào đây.
    @Query("""
    SELECT new com.example.backend.response.WarrantyStatusResponse(
        c.chiTietId, c.soSerial, bt.maSku, sp.tenSanPham, bt.baoHanhThang,
        d.ngayGiaoThucTe, d.maDonHang, kh.hoTen, kh.soDienThoai
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

    // Còn dòng nào KHÁC "trong_kho" (đã bán/giữ hàng/lỗi bảo hành/đã trả) không — dùng để
    // chặn xóa biến thể/sản phẩm nếu đã có serial từng qua giao dịch.
    boolean existsByBienThe_BienTheIdAndTrangThaiNot(Integer bienTheId, String trangThai);

    // Xóa hết serial (chỉ còn "trong_kho" nhờ guard ở trên) khi xóa hẳn biến thể/sản phẩm.
    void deleteByBienThe_BienTheId(Integer bienTheId);
}
