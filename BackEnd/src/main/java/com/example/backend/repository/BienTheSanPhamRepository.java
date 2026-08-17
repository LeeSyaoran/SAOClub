package com.example.backend.repository;

import com.example.backend.entity.BienTheSanPham;
import com.example.backend.response.BienTheSanPhamPublicResponse;
import com.example.backend.response.BienTheSanPhamResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BienTheSanPhamRepository extends JpaRepository<BienTheSanPham, Integer> {

    @Query("""
    SELECT new com.example.backend.response.BienTheSanPhamResponse(
        bt.bienTheId, sp.sanPhamId, bt.maSku, bt.barcode, bt.giaNhap, bt.giaBan,
        bt.baoHanhThang, bt.hinhAnhBienThe, bt.trangThai, bt.mauSac,
        cpu.cpuId, cpu.tenCpu,
        ram.ramId, ram.dungLuong,
        oCung.oCungId, oCung.loaiOcung,
        gpu.gpuId, gpu.tenGpu,
        bt.kichThuocManHinh, bt.heDieuHanh, bt.pin, bt.trongLuongKg
    )
    FROM BienTheSanPham bt
    JOIN bt.sanPham sp
    LEFT JOIN bt.cpu cpu
    LEFT JOIN bt.ram ram
    LEFT JOIN bt.oCung oCung
    LEFT JOIN bt.gpu gpu
    ORDER BY sp.sanPhamId
    """)
    List<BienTheSanPhamResponse> hienThiBienTheSanPham();

    @Query(value = """
    SELECT new com.example.backend.response.BienTheSanPhamResponse(
        bt.bienTheId, sp.sanPhamId, bt.maSku, bt.barcode, bt.giaNhap, bt.giaBan,
        bt.baoHanhThang, bt.hinhAnhBienThe, bt.trangThai, bt.mauSac,
        cpu.cpuId, cpu.tenCpu,
        ram.ramId, ram.dungLuong,
        oCung.oCungId, oCung.loaiOcung,
        gpu.gpuId, gpu.tenGpu,
        bt.kichThuocManHinh, bt.heDieuHanh, bt.pin, bt.trongLuongKg
    )
    FROM BienTheSanPham bt
    JOIN bt.sanPham sp
    LEFT JOIN bt.cpu cpu
    LEFT JOIN bt.ram ram
    LEFT JOIN bt.oCung oCung
    LEFT JOIN bt.gpu gpu
    ORDER BY sp.sanPhamId
    """,
    countQuery = "SELECT COUNT(bt) FROM BienTheSanPham bt JOIN bt.sanPham sp")
    Page<BienTheSanPhamResponse> hienThiBienTheSanPham(Pageable pageable);

    List<BienTheSanPham> findBySanPham_SanPhamId(Integer sanPhamId);

    // Kiểm tra trùng barcode TRƯỚC khi insert/update — cùng lý do với SanPhamRepository.
    boolean existsByBarcode(String barcode);

    boolean existsByBarcodeAndBienTheIdNot(String barcode, Integer bienTheId);

    @Query("""
    SELECT new com.example.backend.response.BienTheSanPhamPublicResponse(
        bt.bienTheId, sp.sanPhamId, bt.maSku, bt.giaBan,
        bt.baoHanhThang, bt.hinhAnhBienThe, bt.trangThai, bt.mauSac,
        cpu.cpuId, cpu.tenCpu,
        ram.ramId, ram.dungLuong,
        oCung.oCungId, oCung.loaiOcung,
        gpu.gpuId, gpu.tenGpu,
        bt.kichThuocManHinh, bt.heDieuHanh, bt.pin, bt.trongLuongKg
    )
    FROM BienTheSanPham bt
    JOIN bt.sanPham sp
    LEFT JOIN bt.cpu cpu
    LEFT JOIN bt.ram ram
    LEFT JOIN bt.oCung oCung
    LEFT JOIN bt.gpu gpu
    ORDER BY sp.sanPhamId
    """)
    List<BienTheSanPhamPublicResponse> hienThiBienTheSanPhamPublic();

    @Query("""
    SELECT new com.example.backend.response.BienTheSanPhamPublicResponse(
        bt.bienTheId, sp.sanPhamId, bt.maSku, bt.giaBan,
        bt.baoHanhThang, bt.hinhAnhBienThe, bt.trangThai, bt.mauSac,
        cpu.cpuId, cpu.tenCpu,
        ram.ramId, ram.dungLuong,
        oCung.oCungId, oCung.loaiOcung,
        gpu.gpuId, gpu.tenGpu,
        bt.kichThuocManHinh, bt.heDieuHanh, bt.pin, bt.trongLuongKg
    )
    FROM BienTheSanPham bt
    JOIN bt.sanPham sp
    LEFT JOIN bt.cpu cpu
    LEFT JOIN bt.ram ram
    LEFT JOIN bt.oCung oCung
    LEFT JOIN bt.gpu gpu
    WHERE bt.bienTheId = :id
    """)
    Optional<BienTheSanPhamPublicResponse> findPublicById(@Param("id") Integer id);

    @Query("""
    SELECT new com.example.backend.response.BienTheSanPhamResponse(
        bt.bienTheId, sp.sanPhamId, bt.maSku, bt.barcode, bt.giaNhap, bt.giaBan,
        bt.baoHanhThang, bt.hinhAnhBienThe, bt.trangThai, bt.mauSac,
        cpu.cpuId, cpu.tenCpu,
        ram.ramId, ram.dungLuong,
        oCung.oCungId, oCung.loaiOcung,
        gpu.gpuId, gpu.tenGpu,
        bt.kichThuocManHinh, bt.heDieuHanh, bt.pin, bt.trongLuongKg
    )
    FROM BienTheSanPham bt
    JOIN bt.sanPham sp
    LEFT JOIN bt.cpu cpu
    LEFT JOIN bt.ram ram
    LEFT JOIN bt.oCung oCung
    LEFT JOIN bt.gpu gpu
    WHERE bt.bienTheId = :id
    """)
    Optional<BienTheSanPhamResponse> findResponseById(@Param("id") Integer id);

    @Query("""
        SELECT COUNT(bt) > 0 FROM BienTheSanPham bt
        WHERE bt.sanPham.sanPhamId = :sanPhamId
        AND (
            EXISTS (SELECT 1 FROM ChiTietDonHang ctdh WHERE ctdh.bienThe.bienTheId = bt.bienTheId)
            OR EXISTS (SELECT 1 FROM ChiTietPhieuNhap ctp WHERE ctp.bienThe.bienTheId = bt.bienTheId)
            OR EXISTS (SELECT 1 FROM ChiTietTraHang ctt WHERE ctt.bienThe.bienTheId = bt.bienTheId)
            OR EXISTS (SELECT 1 FROM PhieuBaoHanh pb WHERE pb.bienThe.bienTheId = bt.bienTheId)
            OR EXISTS (SELECT 1 FROM ChiTietSanPham ctsp WHERE ctsp.bienThe.bienTheId = bt.bienTheId AND ctsp.trangThai <> 'trong_kho')
        )
    """)
    boolean hasTransactionHistoryBySanPhamId(@Param("sanPhamId") Integer sanPhamId);
}
