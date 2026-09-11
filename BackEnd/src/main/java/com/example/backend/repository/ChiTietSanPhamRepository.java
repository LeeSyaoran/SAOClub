package com.example.backend.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.backend.entity.ChiTietDonHang;
import com.example.backend.entity.ChiTietSanPham;
import com.example.backend.response.ChiTietSanPhamResponse;
import com.example.backend.response.WarrantyStatusResponse;

import jakarta.persistence.LockModeType;

@Repository
public interface ChiTietSanPhamRepository extends JpaRepository<ChiTietSanPham, Integer> {
    @Query("""
    SELECT new com.example.backend.response.ChiTietSanPhamResponse(
        c.chiTietId, c.bienThe.bienTheId, pn.phieuNhapId, c.bienThe.maSku,
        c.soSerial, c.trangThai, c.ngayNhapKho, c.ghiChu,
        c.lockedBy, c.lockedAt, c.lockSession,
        CASE WHEN c.lockedBy IS NOT NULL THEN nv.hoTen ELSE NULL END
    )
    FROM ChiTietSanPham c
    LEFT JOIN c.phieuNhap pn
    LEFT JOIN com.example.backend.entity.NhanVien nv ON nv.id = c.lockedBy
    WHERE c.daXoa = false
    """)
    List<ChiTietSanPhamResponse> hienThiChiTietSanPham();

    @Query("""
        SELECT new com.example.backend.response.ChiTietSanPhamResponse(
            c.chiTietId, c.bienThe.bienTheId, pn.phieuNhapId, c.bienThe.maSku,
            c.soSerial, c.trangThai, c.ngayNhapKho, c.ghiChu,
            c.lockedBy, c.lockedAt, c.lockSession,
            CASE WHEN c.lockedBy IS NOT NULL THEN nv.hoTen ELSE NULL END
        )
        FROM ChiTietSanPham c
        LEFT JOIN c.phieuNhap pn
        LEFT JOIN com.example.backend.entity.NhanVien nv ON nv.id = c.lockedBy
        WHERE c.bienThe.bienTheId = :bienTheId AND c.daXoa = false
        """)
    List<ChiTietSanPhamResponse> findByBienTheId(@Param("bienTheId") Integer bienTheId);

    @Query("""
        SELECT new com.example.backend.response.ChiTietSanPhamResponse(
            c.chiTietId, c.bienThe.bienTheId, pn.phieuNhapId, c.bienThe.maSku,
            c.soSerial, c.trangThai, c.ngayNhapKho, c.ghiChu,
            c.lockedBy, c.lockedAt, c.lockSession,
            CASE WHEN c.lockedBy IS NOT NULL THEN nv.hoTen ELSE NULL END
        )
        FROM ChiTietSanPham c
        LEFT JOIN c.phieuNhap pn
        LEFT JOIN com.example.backend.entity.NhanVien nv ON nv.id = c.lockedBy
        WHERE pn.phieuNhapId = :phieuNhapId AND c.daXoa = false
        """)
    List<ChiTietSanPhamResponse> findByPhieuNhapId(@Param("phieuNhapId") Integer phieuNhapId);

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
    WHERE c.trangThai = 'da_ban' AND d.ngayGiaoThucTe IS NOT NULL AND c.daXoa = false
    """)
    List<WarrantyStatusResponse> timSerialDaBanCoGiaoHang();

    boolean existsByBienThe_BienTheIdAndTrangThaiNot(Integer bienTheId, String trangThai);

    // Chi kiem tra serial chua xoa — dung khi tao serial moi
    boolean existsBySoSerialAndDaXoaFalse(String soSerial);

    // Chi kiem tra serial chua xoa — dung khi sua serial
    boolean existsBySoSerialAndChiTietIdNotAndDaXoaFalse(String soSerial, Integer chiTietId);

    void deleteByBienThe_BienTheId(Integer bienTheId);

    // Tìm serial đang 'giu_hang' mà KHÔNG liên kết với bất kỳ chi_tiet_don_hang nào
    // (orphan - xảy ra khi đơn đã bị xóa/hủy nhưng serial đã được set 'giu_hang' từ frontend
    // trước đó, hoặc user đóng tab giữa chừng trước khi đơn được tạo chính thức).
    // Dùng cho action dọn rác tự động (gọi từ ChiTietSanPhamService.hienThiChiTietSanPham
    // mỗi lần load bảng serial). JOIN FETCH bienThe để tránh LazyInitializationException
    // khi service cần ghi lich_su_ton_kho với serial.getBienThe() — đặc biệt vì
    // releaseOrphanSerials() được gọi nội bộ (self-call) nên @Transactional có thể không
    // kích hoạt qua Spring proxy, session đã đóng trước khi lặp.
    // 
    // Kiểm tra cả ChiTietDonHangSerial (many-to-many) và ChiTietDonHang.chiTietSanPham
    // (direct reference) để tránh dọn nhầm serial vừa tạo nhưng ChiTietDonHangSerial chưa
    // được sync/flush trong transaction.
    @Query("""
        SELECT c FROM ChiTietSanPham c
        JOIN FETCH c.bienThe
        WHERE c.trangThai = 'giu_hang' AND c.daXoa = false
          AND NOT EXISTS (
              SELECT 1 FROM ChiTietDonHangSerial s
              WHERE s.chiTietSanPham.chiTietId = c.chiTietId
          )
          AND NOT EXISTS (
              SELECT 1 FROM ChiTietDonHang cdh
              WHERE cdh.chiTietSanPham.chiTietId = c.chiTietId
          )
        """)
    List<ChiTietSanPham> findOrphanGiuHangSerials();

    // Batch check trùng serial trong DB (chỉ serial chưa xóa) — dùng khi duyệt phiếu nhập.
    List<ChiTietSanPham> findBySoSerialInAndDaXoaFalse(Collection<String> soSerials);

    // Lấy tất cả serial (kể cả đã bán/giữ hàng và đã soft-delete) thuộc 1 phiếu nhập.
    // Dùng khi xóa phiếu nhập để quyết định serial nào được phép soft-delete.
    @Query("SELECT c FROM ChiTietSanPham c WHERE c.phieuNhap.phieuNhapId = :phieuNhapId")
    List<ChiTietSanPham> findByPhieuNhap_PhieuNhapIdIncludingDeleted(@Param("phieuNhapId") Integer phieuNhapId);

    // Tra cuu serial chinh xac theo soSerial — chi tra ve serial chua xoa
    // JOIN FETCH BienThe + SanPham de tranh N+1. Tra ve Optional de service xu ly not-found.
    @Query("""
        SELECT c FROM ChiTietSanPham c
        JOIN FETCH c.bienThe bt
        JOIN FETCH bt.sanPham sp
        LEFT JOIN FETCH bt.cpu
        LEFT JOIN FETCH bt.ram
        LEFT JOIN FETCH bt.gpu
        LEFT JOIN FETCH bt.oCung
        WHERE c.soSerial = :soSerial AND c.daXoa = false
        """)
    java.util.Optional<ChiTietSanPham> findBySoSerialWithVariant(@Param("soSerial") String soSerial);

    // Tim theo barcode (bien_the) hoac so_serial (chi_tiet_san_pham) — dung cho tra cuu bao hanh bang ma vach
    // Chi tra ve serial chua bi xoa (da_xoa = false)
    @Query("""
        SELECT c FROM ChiTietSanPham c
        JOIN FETCH c.bienThe
        JOIN FETCH c.bienThe.sanPham
        LEFT JOIN FETCH c.bienThe.cpu
        LEFT JOIN FETCH c.bienThe.ram
        LEFT JOIN FETCH c.bienThe.gpu
        LEFT JOIN FETCH c.bienThe.oCung
        WHERE (c.bienThe.barcode = :barcode OR c.soSerial = :soSerial)
          AND c.daXoa = false
        """)
    java.util.List<ChiTietSanPham> findActiveByBarcodeOrSoSerial(@Param("barcode") String barcode, @Param("soSerial") String soSerial);

    // Kiem tra xem barcode/serial da bi xoa mem chua
    @Query("""
        SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
        FROM ChiTietSanPham c
        WHERE (c.bienThe.barcode = :barcode OR c.soSerial = :soSerial)
          AND c.daXoa = true
        """)
    boolean existsDeletedByBarcodeOrSoSerial(@Param("barcode") String barcode, @Param("soSerial") String soSerial);

    // Lay don hang cua serial (dung cho tra cuu bao hanh) — chi lay moi nhat theo ngay dat
    @Query("""
        SELECT cdh FROM ChiTietDonHang cdh
        JOIN FETCH cdh.donHang d
        JOIN FETCH d.khachHang
        WHERE cdh.chiTietSanPham.chiTietId = :chiTietId
        ORDER BY d.ngayDat DESC
        """)
    java.util.Optional<ChiTietDonHang> findLatestOrderBySerialChiTietId(@Param("chiTietId") Integer chiTietId);

    // ========== SERIAL LOCKING ==========

    // Lock nhiều serial cùng lúc — chỉ lock serial đang 'trong_kho' và chưa bị lock (hoặc lock đã hết hạn)
    // Trả về số serial đã lock được
    @org.springframework.data.jpa.repository.Modifying
    @Query("""
        UPDATE ChiTietSanPham c SET c.lockedBy = :lockedBy, c.lockedAt = :lockedAt,
        c.lockSession = :lockSession
        WHERE c.chiTietId IN :ids AND c.trangThai = 'trong_kho'
        AND (c.lockedBy IS NULL OR c.lockedAt IS NULL
             OR c.lockedAt < :expiredBefore)
        """)
    int lockSerials(@Param("ids") List<Integer> ids,
                     @Param("lockedBy") Integer lockedBy,
                     @Param("lockedAt") LocalDateTime lockedAt,
                     @Param("lockSession") String lockSession,
                     @Param("expiredBefore") LocalDateTime expiredBefore);

    // Unlock nhiều serial — chỉ unlock serial do session này lock
    @org.springframework.data.jpa.repository.Modifying
    @Query("""
        UPDATE ChiTietSanPham c SET c.lockedBy = NULL, c.lockedAt = NULL, c.lockSession = NULL
        WHERE c.chiTietId IN :ids AND c.lockSession = :sessionId
        """)
    int unlockSerials(@Param("ids") List<Integer> ids, @Param("sessionId") String sessionId);

    // Tìm serial đang bị lock (dùng cho hienThiChiTietSanPham — JOIN FETCH)
    @Query("""
        SELECT new com.example.backend.response.ChiTietSanPhamResponse(
            c.chiTietId, c.bienThe.bienTheId, pn.phieuNhapId, c.bienThe.maSku,
            c.soSerial, c.trangThai, c.ngayNhapKho, c.ghiChu,
            c.lockedBy, c.lockedAt, c.lockSession,
            CASE WHEN c.lockedBy IS NOT NULL THEN nv.hoTen ELSE NULL END
        )
        FROM ChiTietSanPham c
        LEFT JOIN c.phieuNhap pn
        LEFT JOIN com.example.backend.entity.NhanVien nv ON nv.id = c.lockedBy
        WHERE c.daXoa = false AND c.lockedBy IS NOT NULL
        ORDER BY c.lockedAt DESC
        """)
    List<ChiTietSanPhamResponse> findLockedSerials();

    // Tìm serial đã hết lock timeout — dùng cho scheduled cleanup
    @Query("SELECT c FROM ChiTietSanPham c WHERE c.lockedAt IS NOT NULL AND c.lockedAt < :expiredBefore")
    List<ChiTietSanPham> findExpiredLocks(@Param("expiredBefore") LocalDateTime expiredBefore);
}
